import axios from "axios";

const API_BASE_URL = "http://localhost:8080";

const axiosInstance = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true, // Important for sessions to work
    headers: {
        "Content-Type": "application/json",
    },
});

export const fetchFacebookConversations = async () => {
    try {
        const response = await axiosInstance.get(`${API_BASE_URL}/conversations`);
        return response.data;
    } catch (error) {
        console.error("Failed to fetch facebook conversations: ", error);
        return null;
    }
};

export const fetchFacebookMessages = async (conversationId) => {
    try {
        const response = await axiosInstance.get(`${API_BASE_URL}/conversation/` + conversationId);
        return response.data;
    } catch (error) {
        console.error("Failed to fetch facebook messages: ", error);
        return null;
    }
};

export const sendFacebookMessage = async (recipientId, messageText) => {
    try {
        const response = await axiosInstance.post(`${API_BASE_URL}/sendMessage/${recipientId}?messageText=` + messageText);
        return response.data;
    } catch (error) {
        console.error("Failed to send facebook message: ", error);
        return null;
    }
}
