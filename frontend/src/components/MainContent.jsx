import React, {useState, useEffect} from 'react';
import {Container, Row, Col, ListGroup, Spinner, FormControl, InputGroup, Button, Alert} from 'react-bootstrap';
import '../App.css';

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

    useEffect(() => {
        resetState();
        fetchConversations();
    }, [selectedChannel, setLoadingConversations]);

    const resetState = () => {
        setConversations([]);
        setSelectedConversation(null);
        setMessages([]);
        setMessage('');
        setRecipient(null);
        setMessageFrom(null);
        setLoadingMessages(false);
        setAlertMessage('');
        setAlertVisible(false);
    };

    const fetchConversations = async () => {
        setLoadingConversations(true);
        try {
            let data = [];
            if (selectedChannel === 'All') {
                const facebookConversations = await fetchFacebookConversations();
                const whatsAppConversations = await fetchWhatsAppConversations();
                const emailConversations = await fetchEmailConversations();

                data = [
                    ...facebookConversations.map((conv) => ({ ...conv, channel: 'Facebook' })),
                    ...whatsAppConversations.map((conv) => ({ ...conv, channel: 'WhatsApp' })),
                    ...emailConversations.map((conv) => ({ ...conv, channel: 'Email' }))
                ];
            } else if (selectedChannel === 'Facebook') {
                const facebookConversations = await fetchFacebookConversations();
                data = facebookConversations.map((conv) => ({ ...conv, channel: 'Facebook' }));
            } else if (selectedChannel === 'WhatsApp') {
                const whatsAppConversations = await fetchWhatsAppConversations();
                data = whatsAppConversations.map((conv) => ({ ...conv, channel: 'WhatsApp' }));
            } else if (selectedChannel === 'Email') {
                const emailConversations = await fetchEmailConversations();
                data = emailConversations.map((conv) => ({ ...conv, channel: 'Email' }));
            }
            setConversations(data);
        } catch (error) {
            console.error('Error fetching conversations:', error);
        } finally {
            setLoadingConversations(false);
        }
    };
    const handleConversationClick = async (conversation, index) => {
        setLoadingMessages(true);
        setSelectedConversation(conversation);
        setMessages([]);

        let conversationId = getConversationId(conversation, index);
        let data = [];
        if (conversation.channel === 'Facebook') {
            data = await fetchFacebookMessages(conversationId);
            handleFacebookConversation(data, conversationId);
        } else if (conversation.channel === 'WhatsApp') {
            data = await fetchWhatsAppMessages(conversationId);
            handleWhatsAppConversation(data);
        } else if (conversation.channel === 'Email') {
            data = await fetchEmailConversations();
            handleEmailConversation(data, conversationId);
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

    const handleEmailConversation = (data, conversationId) => {
        if (selectedChannel === 'All') {
            let conversation = conversations[conversationId];
            // Filter out the actual messages (ignoring 'channel' or any other non-numeric properties)
            let messagesArray = Object.values(conversation).filter((msg) => typeof msg === 'object');
            setMessages(messagesArray);
            setRecipient(messagesArray[0].from);
        } else {
            setMessages(data[conversationId]);
            setRecipient(data[conversationId][0].from);
        }
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

        console.log(response);

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
                            ? conversation[getLastIndexOfEmailConversation(conversation)].sentDate
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

    const getLastIndexOfEmailConversation = (conversation) => {
        const keys = Object.keys(conversation);  // Get all keys from the conversation object
        const numericKeys = keys.filter(key => !isNaN(key)).map(Number);
        return Math.max(...numericKeys);
    }

    const getConversationKey = (conversation, index) => (
        conversation.channel === 'Facebook' ? conversation.id : conversation.from + '' + index
    );

    const getConversationId = (conversation, index) => (
        conversation.channel === 'Facebook' ? conversation.id :
            conversation.channel === 'WhatsApp' ? conversation.sender : index
    );

    const renderConversationHeader = (conversation, dateFormatted) => {
        let sender = 'Unknown Sender';
        if (conversation.channel === 'Facebook' && conversation.participants && conversation.participants[0]) {
            sender = conversation.participants[0].name;
        } else if (conversation.channel === 'WhatsApp') {
            sender = conversation.sender;
        } else if (conversation.channel === 'Email' && conversation[0]) {
            sender = conversation[0].from;
        }

        return <>
            <span className="text-black">{sender} </span>
            {selectedChannel === 'All' && <span className="text-black">({conversation.channel})</span>}
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
        } else if (selectedConversation.channel === 'Email' && typeof message.from === 'string') {
            return message.from || 'Unknown Sender';
        } else if (selectedConversation.channel === 'WhatsApp' && message.sender) {
            return message.sender || 'Unknown Sender';
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
