import axios from "axios";

const API_BASE_URL = "http://localhost:8080";

const axiosInstance = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true, // Important for sessions to work
    headers: {
        "Content-Type": "application/json",
    },
});

export const fetchEmailConversations = async () => {
    try {
        const response = await axiosInstance.get(`${API_BASE_URL}/fetch-emails`);
        return response.data;
    } catch (error) {
        console.error("Failed to fetch email conversations: ", error);
        return null;
    }
}

export const sendEmailMessage = async (recipient, subject, message, messageId) => {
    try {
        const response = await axiosInstance.post(`${API_BASE_URL}/reply-email`, {
            recipient,
            subject,
            message,
            messageId
        });
        return response.data;
    } catch (error) {
        console.error("Failed to send email: ", error);
        return null;
    }
}