import React, { useContext } from 'react';
import { Badge, Nav } from 'react-bootstrap';
import { UnreadMessagesContext } from '../context/UnreadMessagesContext';

const Sidebar = ({ channels, selectedChannel, setSelectedChannel }) => {
    // Access the unread message counts from the context
    const { facebookUnreadCount, whatsAppUnreadCount, emailUnreadCount } = useContext(UnreadMessagesContext);

    return (
        <Nav className="flex-column sidebar" style={{ width: '200px' }}>
            {channels.map((channel) => (
                <Nav.Link
                    className={`${selectedChannel !== channel ? 'text-white' : 'text-primary'}`}
                    key={channel}
                    onClick={() => setSelectedChannel(channel)}
                    style={{ cursor: 'pointer' }}
                >
                    {channel}
                    {channel === "Facebook" && facebookUnreadCount > 0 && (
                        <Badge bg="danger" pill style={{ marginLeft: "15px" }}>
                            {facebookUnreadCount}
                        </Badge>
                    )}
                    {channel === "WhatsApp" && whatsAppUnreadCount > 0 && (
                        <Badge bg="danger" pill style={{ marginLeft: "15px" }}>
                            {whatsAppUnreadCount}
                        </Badge>
                    )}
                    {channel === "Email" && emailUnreadCount > 0 && (
                        <Badge bg="danger" pill style={{ marginLeft: "15px" }}>
                            {emailUnreadCount}
                        </Badge>
                    )}
                </Nav.Link>
            ))}
        </Nav>
    );
};

export default Sidebar;