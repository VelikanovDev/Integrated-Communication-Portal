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

const MainContent = ({selectedChannel, loadingConversations, setLoadingConversations}) => {
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
            if (selectedChannel === 'Facebook') {
                data = await fetchFacebookConversations();
            } else if (selectedChannel === 'WhatsApp') {
                data = await fetchWhatsAppConversations();
            } else if (selectedChannel === 'Email') {
                data = await fetchEmailConversations();
            }
            setConversations(data);
        } catch (error) {
            console.error('Error fetching conversations:', error);
        } finally {
            setLoadingConversations(false);
        }
    };

    const handleConversationClick = async (conversationId) => {
        setLoadingMessages(true);
        setSelectedConversation(conversationId);
        setMessages([]);

        let data = [];
        if (selectedChannel === 'Facebook') {
            data = await fetchFacebookMessages(conversationId);
            handleFacebookConversation(data, conversationId);
        } else if (selectedChannel === 'WhatsApp') {
            data = await fetchWhatsAppMessages(conversationId);
            handleWhatsAppConversation(data);
        } else if (selectedChannel === 'Email') {
            data = await fetchEmailConversations(conversationId);
            handleEmailConversation(data, conversationId);
        }
        setLoadingMessages(false);
        setAlertMessage('');
        setAlertVisible(false);
    };

    const handleFacebookConversation = (data, conversationId) => {
        setMessages(data);
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
        setMessages(data[conversationId]);
        setRecipient(data[conversationId][0].from);
    };

    const handleSendMessage = async () => {
        let newMessage = null;
        if (selectedChannel === 'Facebook' && message.trim()) {
            newMessage = await handleFacebookSendMessage();
            setMessages((prevMessages) => [newMessage, ...prevMessages])
        } else if (selectedChannel === 'WhatsApp' && message.trim()) {
            newMessage = await handleWhatsAppSendMessage();

            if (newMessage === null) {
                return;
            }

            setMessages((prevMessages) => [...prevMessages, newMessage])
        } else if (selectedChannel === 'Email' && message.trim()) {
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
                const conversationUpdatedTime = selectedChannel === 'Facebook'
                    ? conversation.updatedTime
                    : selectedChannel === 'WhatsApp'
                        ? conversation.lastMessageDate
                        : selectedChannel === 'Email'
                            ? conversation[conversation.length - 1].sentDate
                            : "";
                const dateFormatted = formatDateFromString(conversationUpdatedTime);
                return (
                    <ListGroup.Item
                        key={getConversationKey(conversation, index)}
                        action
                        onClick={() => {
                            if (selectedConversation !== getConversationId(conversation, index)) {
                                handleConversationClick(getConversationId(conversation, index));
                            }
                        }}
                        active={selectedConversation === getConversationId(conversation, index)}
                    >
                        {renderConversationHeader(conversation, dateFormatted)}
                    </ListGroup.Item>
                );
            })}
        </ListGroup>
    );

    const getConversationKey = (conversation, index) => (
        selectedChannel === 'Facebook' ? conversation.id : conversation.from + '' + index
    );

    const getConversationId = (conversation, index) => (
        selectedChannel === 'Facebook' ? conversation.id : selectedChannel === 'WhatsApp' ? conversation.sender : index
    );

    const renderConversationHeader = (conversation, dateFormatted) => {
        if (selectedChannel === 'Facebook' && conversation.participants && conversation.participants[0]) {
            return <>
                {conversation.participants[0].name}
                <br/>
                {dateFormatted}
            </>;
        } else if (selectedChannel === 'WhatsApp') {
            return <>
                {conversation.sender}
                <br/>
                {dateFormatted}
            </>;
        } else if (selectedChannel === 'Email' && conversation[0] && typeof conversation[0].from === 'string') {
            return <>
                {conversation[0].from}
                <br/>
                {dateFormatted}
            </>;
        }
        return 'Unknown Sender';
    };

    const renderMessages = () => (
        <ListGroup>
            {selectedConversation === null ? '' :
                (messages.length === 0 ? '' : (
                    selectedChannel === 'Facebook' ? messages.slice().reverse() : messages
                ).map((message, index) => {
                    const isSent = selectedChannel === 'Facebook'
                        ? message.from?.name === messageFrom
                        : selectedChannel === 'WhatsApp'
                            ? message.sender === messageFrom
                            : selectedChannel === 'Email'
                                ? message.from === messages[0]?.to
                                : false;

                    const messageClass = isSent ? 'message-sent' : 'message-received';

                    return (
                        <ListGroup.Item key={`${selectedConversation}-${index}`} className={messageClass}>
                            <strong>{renderMessageSender(message)}</strong>{': '}
                            {renderMessageContent(message)}
                        </ListGroup.Item>
                    );
                }))}
            {selectedConversation === null ? '' :
                (<InputGroup className="mb-3">
                    <FormControl
                        aria-label="Example text with button addon"
                        aria-describedby="basic-addon1"
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                    />
                    <Button variant="primary" id="button-addon1" onClick={handleSendMessage}>
                        Send
                    </Button>
                </InputGroup>)}
        </ListGroup>
    );

    const renderMessageSender = (message) => {
        if (selectedChannel === 'Facebook' && message.from && typeof message.from.name === 'string') {
            return message.from.name || 'Unknown Sender';
        } else if (selectedChannel === 'Email' && typeof message.from === 'string') {
            return message.from || 'Unknown Sender';
        } else if (selectedChannel === 'WhatsApp' && message.sender) {
            return message.sender || 'Unknown Sender';
        }
        return 'Unknown Sender';
    };

    const renderMessageContent = (message) => {
        if (selectedChannel === 'Facebook' && message.message) {
            return message.message;
        } else if (selectedChannel === 'Email' && message.body) {
            return message.body;
        } else if (selectedChannel === 'WhatsApp' && message.message) {
            return message.message;
        }
        return '';
    };

    const renderContent = () => {
        if (selectedChannel === 'Facebook' || selectedChannel === 'Email' || selectedChannel === 'WhatsApp') {
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
