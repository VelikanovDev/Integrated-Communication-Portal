import React, { useContext } from 'react';
import { Badge, Nav } from 'react-bootstrap';
import { UnreadMessagesContext } from '../context/UnreadMessagesContext'; // Adjust the path as necessary

const Sidebar = ({ channels, selectedChannel, setSelectedChannel }) => {
    // Access the unread message counts from the context
    const { facebookUnreadCount, allUnreadCount } = useContext(UnreadMessagesContext);

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
                    {channel === "All" && allUnreadCount > 0 && (
                        <Badge bg="danger" pill style={{ marginLeft: "15px" }}>
                            {allUnreadCount}
                        </Badge>
                    )}
                    {channel === "Facebook" && facebookUnreadCount > 0 && (
                        <Badge bg="danger" pill style={{ marginLeft: "15px" }}>
                            {facebookUnreadCount}
                        </Badge>
                    )}
                </Nav.Link>
            ))}
        </Nav>
    );
};

export default Sidebar;