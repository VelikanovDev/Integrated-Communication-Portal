import React, {useState, useEffect, useContext, useCallback} from 'react';
import {Container, Row, Col, ListGroup, Spinner, FormControl, InputGroup, Button, Alert, Badge} from 'react-bootstrap';
import '../App.css';
import { UnreadMessagesContext } from "../context/UnreadMessagesContext";


import {
    fetchFacebookConversations,
    fetchFacebookMessages,
    sendFacebookMessage
} from '../services/FacebookService';
import {
    fetchEmailConversations,
    sendEmailMessage
} from '../services/EmailService';
import {
    fetchWhatsAppConversations,
    fetchWhatsAppMessages,
    sendWhatsAppMessage
} from "../services/WhatsAppService";

const MainContent = ({channels, selectedChannel, loadingConversations, setLoadingConversations}) => {
    const [conversations, setConversations] = useState([]);
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [messages, setMessages] = useState([]);
    const [message, setMessage] = useState('');
    const [recipient, setRecipient] = useState(null);
    const [messageFrom, setMessageFrom] = useState(null);
    const [loadingMessages, setLoadingMessages] = useState(false);

    // State for showing alerts
    const [alertVisible, setAlertVisible] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');
    const [alertVariant, setAlertVariant] = useState('danger');

    const {facebookNotifications, whatsappNotifications, emailNotifications, markConversationAsRead } = useContext(UnreadMessagesContext);

    const resetState = useCallback(() => {
        setConversations([]);
        setSelectedConversation(null);
        setMessages([]);
        setMessage('');
        setRecipient(null);
        setMessageFrom(null);
        setLoadingMessages(false);
        setAlertMessage('');
        setAlertVisible(false);
    }, []);

    const fetchConversations = useCallback(async () => {
        setLoadingConversations(true);
        try {
            let data = [];
            switch (selectedChannel) {
                case 'All':
                    const [facebookConversations, whatsAppConversations, emailConversations] = await Promise.all([
                        fetchFacebookConversations(),
                        fetchWhatsAppConversations(),
                        fetchEmailConversations(),
                    ]);
                    data = [
                        ...facebookConversations.map((conv) => ({ ...conv, channel: 'Facebook' })),
                        ...whatsAppConversations.map((conv) => ({ ...conv, channel: 'WhatsApp' })),
                        ...emailConversations.map((conv) => ({ ...conv, channel: 'Email' })),
                    ];
                    break;

                case 'Facebook':
                    const fbConversations = await fetchFacebookConversations();
                    data = fbConversations.map((conv) => ({ ...conv, channel: 'Facebook' }));
                    break;

                case 'WhatsApp':
                    const waConversations = await fetchWhatsAppConversations();
                    data = waConversations.map((conv) => ({ ...conv, channel: 'WhatsApp' }));
                    break;

                case 'Email':
                    const emConversations = await fetchEmailConversations();
                    data = emConversations.map((conv) => ({ ...conv, channel: 'Email' }));
                    break;

                default:
                    break;
            }
            setConversations(data);
        } catch (error) {
            console.error('Error fetching conversations:', error);
            setAlertMessage('Failed to load conversations. Please try again later.');
            setAlertVariant('danger');
            setAlertVisible(true);
        } finally {
            setLoadingConversations(false);
        }
    }, [selectedChannel, setLoadingConversations]);

    useEffect(() => {
        resetState();
        fetchConversations();
    }, [selectedChannel, fetchConversations, resetState]);

    const handleConversationClick = async (conversation, index) => {
        setLoadingMessages(true);
        setSelectedConversation(conversation);
        setMessages([]);

        let conversationId = getConversationId(conversation, index);
        markConversationAsRead(conversationId, conversation.channel);

        let data = [];
        if (conversation.channel === 'Facebook') {
            data = await fetchFacebookMessages(conversationId);
            handleFacebookConversation(data, conversationId);
        } else if (conversation.channel === 'WhatsApp') {
            data = await fetchWhatsAppMessages(conversationId);
            handleWhatsAppConversation(data);
        } else if (conversation.channel === 'Email') {
            handleEmailConversation(index);
        }

        setLoadingMessages(false);
        setAlertMessage('');
        setAlertVisible(false);
    };
    const handleFacebookConversation = (data, conversationId) => {
        setMessages(data.reverse().slice());
        const conversation = conversations.find((conv) => conv.id === conversationId);
        if (conversation && conversation.participants && conversation.participants[0]) {
            setRecipient(conversation.participants[0].id);
            setMessageFrom(conversation.participants[1].name);
        } else {
            setRecipient(null);
        }
    };

    const handleWhatsAppConversation = (data) => {
        setMessages(data);
        setRecipient(data[0].sender);
        setMessageFrom(data[0].recipient);
    };

    const handleEmailConversation = (conversationId) => {
        setMessages(conversations[conversationId].emails);
        setRecipient(conversations[conversationId].sender);
    };

    const handleSendMessage = async () => {
        let newMessage = null;
        if (selectedConversation.channel === 'Facebook' && message.trim()) {
            newMessage = await handleFacebookSendMessage();
            setMessages((prevMessages) => [...prevMessages, newMessage])
        } else if (selectedConversation.channel === 'WhatsApp' && message.trim()) {
            newMessage = await handleWhatsAppSendMessage();

            if (newMessage === null) {
                return;
            }

            setMessages((prevMessages) => [...prevMessages, newMessage])
        } else if (selectedConversation.channel === 'Email' && message.trim()) {
            newMessage = await handleEmailSendMessage();
            setMessages((prevMessages) => [...prevMessages, newMessage])
        }
        setMessage('');

        setAlertMessage('Message sent successfully!');
        setAlertVariant('success');
        setAlertVisible(true);
    };

    const handleAlertDismiss = () => {
        setAlertVisible(false);
    };

    const handleFacebookSendMessage = async () => {
        const response = await sendFacebookMessage(recipient, message);
        return response ? {
            from: {name: messageFrom},
            message,
            timestamp: new Date().toISOString()
        } : null;
    };

    const handleWhatsAppSendMessage = async () => {
        const response = await sendWhatsAppMessage(recipient, message);

        if (response.error) {
            setAlertMessage(response.error);
            setAlertVariant('danger');
            setAlertVisible(true);
            setMessage('');
            return null;
        }

        return {
            sender: messageFrom,
            message,
            timestamp: new Date().toISOString()
        };
    };

    const handleEmailSendMessage = async () => {
        const myEmail = messages[0].to;
        const messageId = messages[0].messageId;
        const subject = `RE: ${messages[0].subject}`;
        const response = await sendEmailMessage(recipient, subject, message, messageId);
        return response ? {
            from: myEmail,
            body: message
        } : null;
    };

    const formatDateFromString = (dateString) => {
        const date = new Date(dateString);
        return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')} ${String(date.getUTCHours()).padStart(2, '0')}:${String(date.getUTCMinutes()).padStart(2, '0')}:${String(date.getUTCSeconds()).padStart(2, '0')}`;
    };

    const renderConversations = () => (
        <ListGroup>
            {conversations.map((conversation, index) => {
                const conversationUpdatedTime = conversation.channel === 'Facebook'
                    ? conversation.updatedTime
                    : conversation.channel === 'WhatsApp'
                        ? conversation.lastMessageDate
                        : conversation.channel === 'Email'
                            ? conversation.lastEmailDate
                            : "";
                const dateFormatted = formatDateFromString(conversationUpdatedTime);

                return (
                    <ListGroup.Item
                        key={getConversationKey(conversation, index)}
                        action
                        onClick={() => {
                            if (selectedConversation !== getConversationId(conversation, index)) {
                                handleConversationClick(conversation, index);
                            }
                        }}
                        active={selectedConversation === conversation}
                    >
                        {renderConversationHeader(conversation, dateFormatted)}
                    </ListGroup.Item>
                );
            })}
        </ListGroup>
    );

    const getConversationKey = (conversation, index) => (
        conversation.channel === 'Facebook' ? conversation.id : conversation.from + '' + index
    );

    const getConversationId = (conversation, index) => (
        conversation.channel === 'Facebook' ? conversation.id :
            conversation.channel === 'WhatsApp' ? conversation.sender :
                conversation.channel === 'Email' ? conversation.conversationId : null
    );

    const renderConversationHeader = (conversation, dateFormatted) => {
        let sender = 'Unknown Sender';
        let unreadCount = 0;

        if (conversation.channel === 'Facebook' && conversation.participants && conversation.participants[0]) {
            sender = conversation.participants[0].name;

            // Find the unread count for Facebook notifications
            const notification = facebookNotifications.find(
                (notif) => notif.participantName === sender
            );

            unreadCount = notification ? notification.newMessagesCount : 0;
        } else if (conversation.channel === 'WhatsApp') {
            sender = conversation.sender;

            // Find the unread count for Facebook notifications
            const notification = whatsappNotifications.find(
                (notif) => notif.sender === sender
            );
            unreadCount = notification ? notification.unreadCount : 0;
        } else if (conversation.channel === 'Email') {
            sender = conversation.sender;
            const conversationId = conversation.conversationId;

            // Find the unread count for Facebook notifications
            const notification = emailNotifications.find(
                (notif) => notif.conversationId === conversationId
            );
            unreadCount = notification ? notification.unreadCount : 0;
        }

        return <>
            <span className="text-black">{sender} </span>
            {selectedChannel === 'All' && <span className="text-black">({conversation.channel})</span>}
            {unreadCount > 0 && (
                <Badge bg="danger" pill style={{ marginLeft: "5px" }}>
                    {unreadCount}
                </Badge>
            )}
            <br />
            <span className="text-black">{dateFormatted}</span>
        </>;
    };
    const renderMessages = () => (
        <ListGroup className="messages-container">
            {selectedConversation === null ? '' :
                (messages.length === 0 ? '' : messages.map((message, index) => {
                    const isSent = selectedConversation.channel === 'Facebook'
                        ? message.from?.name === messageFrom
                        : selectedConversation.channel === 'WhatsApp'
                            ? message.sender === messageFrom
                            : selectedConversation.channel === 'Email'
                                ? message.from === messages[0]?.to
                                : false;

                    const messageClass = isSent ? 'message-sent' : 'message-received';

                    return (
                        <ListGroup.Item key={`${selectedConversation}-${index}`} className={messageClass}>
                            <strong>{renderMessageSender(selectedConversation, message)}</strong>{': '}
                            {renderMessageContent(selectedConversation, message)}
                        </ListGroup.Item>
                    );
                }))}
            {selectedConversation === null ? '' :
                (<InputGroup className="mb-3">
                    <FormControl
                        aria-label="Message"
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                                handleSendMessage();
                            }
                        }}
                    />
                    <Button variant="primary" onClick={handleSendMessage}>
                        Send
                    </Button>
                </InputGroup>)}
        </ListGroup>
    );
    const renderMessageSender = (selectedConversation, message) => {
        if (selectedConversation.channel === 'Facebook' && message.from && typeof message.from.name === 'string') {
            return message.from.name || 'Unknown Sender';
        } else if (selectedConversation.channel === 'WhatsApp' && message.sender) {
            return message.sender || 'Unknown Sender';
        } else if (selectedConversation.channel === 'Email') {
            return message.from || 'Unknown Sender';
        }
        return 'Unknown Sender';
    };

    const renderMessageContent = (selectedConversation, message) => {
        if (selectedConversation.channel === 'Facebook' && message.message) {
            return message.message;
        } else if (selectedConversation.channel === 'Email' && message.body) {
            return message.body;
        } else if (selectedConversation.channel === 'WhatsApp' && message.message) {
            return message.message;
        }
        return '';
    };

    const renderContent = () => {
        if (channels.includes(selectedChannel)) {
            return (
                <Row>
                    <Col xs={3} style={{
                        position: 'sticky',
                        top: '0',
                        height: '100vh',
                        overflowY: 'auto',
                    }}>
                        <h3>Conversations</h3>
                        {loadingConversations ? <Spinner animation="border"/> : renderConversations()}
                    </Col>
                    <Col xs={9}>
                        {recipient !== null ? <h3>Messages</h3> : ''}
                        {alertVisible && (
                            <Alert variant={alertVariant} onClose={handleAlertDismiss} dismissible>
                                {alertMessage}
                            </Alert>
                        )}
                        {loadingMessages ? <Spinner animation="border"/> : renderMessages()}
                    </Col>
                </Row>
            );
        }
        return <h1>{selectedChannel ? selectedChannel : 'Select a channel'}</h1>;
    };

    return <Container className="p-3">{renderContent()}</Container>;
};

export default MainContent;
