import React, { createContext, useState, useEffect } from "react";
import {
    fetchWhatsAppConversations,
    getCountOfWhatsAppUnreadMessages,
    markWhatsAppConversationAsRead
} from "../services/WhatsAppService";

export const UnreadMessagesContext = createContext();

const UnreadMessagesProvider = ({ children }) => {
    const [facebookUnreadCount, setFacebookUnreadCount] = useState(0);
    const [whatsAppUnreadCount, setWhatsAppUnreadCount] = useState(0);
    const [allUnreadCount, setAllUnreadCount] = useState(0);
    const [facebookNotifications, setFacebookNotifications] = useState([]);
    const [whatsappNotifications, setWhatsAppNotifications] = useState([]);

    useEffect(() => {
        const facebookSource = new EventSource("http://localhost:8080/facebook/notifications");

        facebookSource.addEventListener("facebookConversations", (event) => {
            try {
                const data = JSON.parse(event.data);

                // Initialize total unread messages count
                let totalNewMessages = 0;

                const updatedNotifications = data.map((conversation) => {
                    const storedData = JSON.parse(localStorage.getItem(conversation.id)) || {};
                    const previousCount = storedData.messagesFromPrimaryParticipant || 0;

                    // Calculate the new messages count
                    const newMessagesCount = Math.max(
                        0,
                        conversation.messagesFromPrimaryParticipant - previousCount
                    );

                    // Update the total unread count
                    totalNewMessages += newMessagesCount;

                    return {
                        ...conversation,
                        hasNewMessages: newMessagesCount > 0,
                        newMessagesCount,
                    };
                });

                // Update state
                setFacebookNotifications(updatedNotifications);

                // Update total unread count
                setFacebookUnreadCount(totalNewMessages);
                setAllUnreadCount((prev) => prev + totalNewMessages); // Update for 'All' channel if needed
            } catch (error) {
                console.error("Error parsing notifications data:", error);
            }
        });

        // WhatsApp SSE Connection
        const whatsappSource = new EventSource("http://localhost:8080/whatsapp/notifications");

        whatsappSource.addEventListener("whatsappConversations", (event) => {
            try {
                const data = JSON.parse(event.data);


                // Initialize total unread messages count
                let totalNewMessages = 0;

                const updatedWhatsAppNotifications = data.map((conversation) => {
                    console.log(conversation)
                    // Retrieve the stored conversation data
                    const storedData = JSON.parse(localStorage.getItem(conversation.sender)) || { unreadCount: 0 };

                    // Update the total unread count
                    totalNewMessages += conversation.unreadCount;

                    // Save updated conversation data to local storage
                    localStorage.setItem(
                        conversation.sender,
                        JSON.stringify({
                            ...conversation, // Save the full conversation data
                            unreadCount: conversation.unreadCount,
                        })
                    );

                    // Return the updated conversation with new messages count
                    return {
                        ...conversation,
                        hasNewMessages: totalNewMessages > 0,
                        totalNewMessages,
                    };
                });

                // Update state
                setWhatsAppNotifications(updatedWhatsAppNotifications);

                console.log(totalNewMessages + " totalNewMessages");
                // Update WhatsApp unread count
                setWhatsAppUnreadCount(totalNewMessages);

                // Update the total unread count for all channels
                setAllUnreadCount((prevCount) => {
                    const previousWhatsAppCount = prevCount - whatsAppUnreadCount; // Subtract previous WhatsApp unread count
                    return previousWhatsAppCount + totalNewMessages; // Add the updated WhatsApp unread count
                });
            } catch (error) {
                console.error("Error parsing WhatsApp notifications data:", error);
            }
        });

        return () => {
            facebookSource.close();
            whatsappSource.close();
        };
    }, []); // Empty dependency array ensures this runs only once;

    const markConversationAsRead = async (conversationId, conversationChannel) => {
        if (conversationChannel === "Facebook") {
            const conversation = facebookNotifications.find((notif) => notif.id === conversationId);

            if (conversation) {
                // Update local storage for the specific conversation
                localStorage.setItem(
                    conversationId,
                    JSON.stringify({
                        messagesFromPrimaryParticipant: conversation.messagesFromPrimaryParticipant,
                    })
                );

                // Update state to reflect the changes
                setFacebookNotifications((prevNotifications) =>
                    prevNotifications.map((notif) =>
                        notif.id === conversationId
                            ? {...notif, hasNewMessages: false, newMessagesCount: 0}
                            : notif
                    )
                );

                // Recalculate unread counts
                const updatedUnreadCount = facebookNotifications.reduce(
                    (sum, notif) => sum + (notif.hasNewMessages ? notif.newMessagesCount : 0),
                    0
                );
                setFacebookUnreadCount(updatedUnreadCount);
                setAllUnreadCount((prevCount) => prevCount - conversation.newMessagesCount);
            }
        } else if (conversationChannel === "WhatsApp") {
            const conversations = await fetchWhatsAppConversations();
            const conversation = conversations.find((conv) => conv.sender === conversationId);

            if (conversation) {
                // Update local storage for the specific WhatsApp conversation
                localStorage.setItem(
                    conversationId,
                    JSON.stringify({
                        unreadCount: 0,
                    })
                );
                await markWhatsAppConversationAsRead(conversationId).then(r => {
                });

                // Update state to reflect the changes
                setWhatsAppNotifications((prevNotifications) =>
                    prevNotifications.map((notif) =>
                        notif.sender === conversationId
                            ? {...notif, newMessagesCount: 0}
                            : notif
                    )
                );

                const updatedUnreadCount = await getCountOfWhatsAppUnreadMessages();

                setWhatsAppUnreadCount(updatedUnreadCount);
            }
        } else {
            console.warn(`Unsupported channel: ${conversationChannel}`);
        }
    };

    return (
        <UnreadMessagesContext.Provider
            value={{ facebookUnreadCount, whatsappUnreadCount: whatsAppUnreadCount, whatsappNotifications, allUnreadCount, facebookNotifications, markConversationAsRead }}
        >
            {children}
        </UnreadMessagesContext.Provider>
    );
};

export default UnreadMessagesProvider;