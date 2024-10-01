import React from 'react';
import {Nav} from 'react-bootstrap';

const Sidebar = ({channels, selectedChannel, setSelectedChannel}) => {
    return (
        <Nav className="flex-column sidebar" style={{width: '200px'}}>
            {channels.map((channel) => (
                <Nav.Link
                    className={`${selectedChannel !== channel ? 'text-white' : 'text-primary'}`}
                    key={channel}
                    onClick={() => setSelectedChannel(channel)}
                    style={{cursor: 'pointer'}}
                >
                    {channel}
                </Nav.Link>
            ))}
        </Nav>
    );
};

export default Sidebar;
