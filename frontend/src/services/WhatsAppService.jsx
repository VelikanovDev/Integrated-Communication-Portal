import axios from "axios";

const API_BASE_URL = "http://localhost:8080/whatsapp";

const axiosInstance = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true, // Important for sessions to work
    headers: {
        "Content-Type": "application/json",
    },
});

export const fetchWhatsAppConversations = async () => {
    try {
        const response = await axiosInstance.get(`${API_BASE_URL}/conversations`);
        return response.data;
    } catch (error) {
        console.error("Failed to fetch facebook conversations: ", error);
        return null;
    }
};

export const fetchWhatsAppMessages = async (sender) => {
    try {
        const response = await axiosInstance.get(`${API_BASE_URL}/messages/` + sender);
        return response.data;
    } catch (error) {
        console.error("Failed to fetch facebook messages: ", error);
        return null;
    }
}

export const sendWhatsAppMessage = async (recipient, message) => {
    try {
        const response = await axiosInstance.post(`${API_BASE_URL}/sendMessage/${recipient}`, {
            messageText: message
        });
        return response.data;
    } catch (error) {

        // Check if the error response is 400
        if (error.response && error.response.status === 400) {
            return {error: error.response.data || 'Bad Request: Please check your input.'};
        }

        // For other errors
        return {error: 'An unexpected error occurred. Please try again.'};
    }
};

export const markWhatsAppConversationAsRead = async (sender) => {
    try {
        await axios.put(`http://localhost:8080/whatsapp/mark-as-read/${sender}`);
    } catch (error) {
        console.error("Failed to mark WhatsApp conversation as read: ", error);
    }
};

export const getCountOfWhatsAppUnreadMessages = async () => {
    try {
        const response = await axiosInstance.get(`${API_BASE_URL}/unread-messages`);
        return response.data.length;
    } catch (error) {
        console.error("Failed to fetch WhatsApp unread count: ", error);
        return null;
    }
}