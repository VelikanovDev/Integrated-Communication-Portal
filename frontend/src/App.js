import './App.css';
import {Col, Container, Row} from "react-bootstrap";
import MainContent from "./components/MainContent";
import Sidebar from "./components/Sidebar";
import {useState} from "react";


const App = () => {
    const [selectedChannel, setSelectedChannel] = useState(null);
    const [loadingConversations, setLoadingConversations] = useState(false);

    const channels = ['All', 'Facebook', 'WhatsApp', 'Email'];

    const handleChannelSelect = (channel) => {
        if (selectedChannel === channel) {
            return;
        }

        setLoadingConversations(true);
        setSelectedChannel(channel);
    }

    return (
        <Container fluid className="vh-100">
            <Row className="h-100">
                <Col xs="auto" style={{backgroundColor: "#171721"}}>
                    <Sidebar channels={channels} selectedChannel={selectedChannel}
                             setSelectedChannel={handleChannelSelect}/>
                </Col>
                <Col style={{backgroundColor: "#33334a"}}>
                    <MainContent
                        selectedChannel={selectedChannel}
                        loadingConversations={loadingConversations}
                        setLoadingConversations={setLoadingConversations}
                    />
                </Col>
            </Row>
        </Container>
    );
};

export default App;
