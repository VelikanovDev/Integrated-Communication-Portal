import React, { createContext, useState, useEffect } from "react";

export const UnreadMessagesContext = createContext();

const UnreadMessagesProvider = ({ children }) => {
    const [facebookUnreadCount, setFacebookUnreadCount] = useState(0);
    const [allUnreadCount, setAllUnreadCount] = useState(0);
    const [facebookNotifications, setFacebookNotifications] = useState([]);

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
                setAllUnreadCount(totalNewMessages); // Update for 'All' channel if needed
            } catch (error) {
                console.error("Error parsing notifications data:", error);
            }
        });

        return () => {
            facebookSource.close(); // Clean up the EventSource on component unmount
        };
    }, []); // Empty dependency array ensures this runs only once;

    const markConversationAsRead = (conversationId) => {
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
                        ? { ...notif, hasNewMessages: false, newMessagesCount: 0 }
                        : notif
                )
            );

            // Recalculate unread counts
            const updatedUnreadCount = facebookNotifications.reduce(
                (sum, notif) => sum + (notif.hasNewMessages ? notif.newMessagesCount : 0),
                0
            );
            setFacebookUnreadCount(updatedUnreadCount);
            setAllUnreadCount(updatedUnreadCount);
        }
    };

    return (
        <UnreadMessagesContext.Provider
            value={{ facebookUnreadCount, allUnreadCount, facebookNotifications, markConversationAsRead }}
        >
            {children}
        </UnreadMessagesContext.Provider>
    );
};

export default UnreadMessagesProvider;