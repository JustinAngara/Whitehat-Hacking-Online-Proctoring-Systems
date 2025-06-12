import React, { useEffect, useRef, useState } from "react";
import styled, { keyframes } from "styled-components";


// Animations
const pulse = keyframes`
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
`;

const slideIn = keyframes`
  from { transform: translateX(-10px); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
`;

// Styled Components
const Container = styled.div`
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a2e 100%);
  min-height: 100vh;
  font-family: 'Fira Code', 'Monaco', 'Cascadia Code', monospace;
  color: #e0e6ed;
  display: flex;
  flex-direction: column;
`;

const Button = styled.button`
  padding: 0.75rem 1.25rem;
  background: linear-gradient(135deg, #64ffda 0%, #4fc3f7 100%);
  border: none;
  border-radius: 8px;
  color: #0f0f23;
  font-family: inherit;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 1rem;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(100, 255, 218, 0.3);
  }

  &:active {
    transform: translateY(0);
  }
`;

const MessagesContainer = styled.div`
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid #2a2a3e;
  border-radius: 12px;
  height: 400px;
  overflow-y: auto;
  padding: 1rem;
  margin-bottom: 1rem;
  backdrop-filter: blur(10px);
  flex-grow: 1;
`;

const MessageItem = styled.div`
  animation: ${slideIn} 0.3s ease-out;
  margin-bottom: 1rem;
`;

const MessageHeader = styled.div<{ type: 'system' | 'user' | 'server' }>`
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
  font-size: 0.75rem;
  color: ${props => {
    switch (props.type) {
      case 'system': return '#ffd700';
      case 'user': return '#64ffda';
      case 'server': return '#ff9800';
      default: return '#e0e6ed';
    }
  }};
`;

const MessagePrefix = styled.span<{ type: 'system' | 'user' | 'server' }>`
  font-weight: 600;
  text-transform: uppercase;
  font-size: 0.7rem;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  background: ${props => {
    switch (props.type) {
      case 'system': return 'rgba(255, 215, 0, 0.1)';
      case 'user': return 'rgba(100, 255, 218, 0.1)';
      case 'server': return 'rgba(255, 152, 0, 0.1)';
      default: return 'rgba(255, 255, 255, 0.1)';
    }
  }};
  border: 1px solid ${props => {
    switch (props.type) {
      case 'system': return 'rgba(255, 215, 0, 0.2)';
      case 'user': return 'rgba(100, 255, 218, 0.2)';
      case 'server': return 'rgba(255, 152, 0, 0.2)';
      default: return 'rgba(255, 255, 255, 0.2)';
    }
  }};
`;

const MessageContent = styled.div`
  padding: 0.5rem 0;
  line-height: 1.4;
  word-wrap: break-word;
`;

const MessageTimestamp = styled.span`
  color: #666;
  font-size: 0.7rem;
`;

const InputContainer = styled.div`
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-top: 1rem;
`;

const Input = styled.input`
  flex: 1;
  padding: 0.875rem 1rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid #2a2a3e;
  border-radius: 8px;
  color: #e0e6ed;
  font-family: inherit;
  font-size: 0.875rem;
  transition: all 0.2s ease;

  &:focus {
    outline: none;
    border-color: #64ffda;
    box-shadow: 0 0 0 2px rgba(100, 255, 218, 0.1);
    background: rgba(255, 255, 255, 0.08);
  }

  &::placeholder {
    color: #666;
  }
`;

const WebSocketClient: React.FC = () => {
  const ws = useRef<WebSocket | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState("");
  const [isConnected, setIsConnected] = useState(false);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const skipScrollRef = useRef(false);

  const scrollToBottom = () => {
    if (!skipScrollRef.current) {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    } else {
      skipScrollRef.current = false;
    }
  };

  useEffect(scrollToBottom, [messages]);

  const connectWebSocket = () => {
    skipScrollRef.current = true; // Prevent scroll on reconnect

    if (ws.current && ws.current.readyState < 2) {
      ws.current.close();
    }

    setIsReconnecting(true);
    setIsConnected(false);
    appendMessage("Attempting to connect to server...", 'system');

    try {
      const newWS = new WebSocket("ws://localhost:8080/ws");

      newWS.onopen = () => {
        setIsConnected(true);
        setIsReconnecting(false);
        appendMessage("Connected to Java WebSocket server", 'system');
      };

      newWS.onmessage = (event) => {
        appendMessage(event.data, 'server');
      };

      newWS.onerror = () => {
        setIsConnected(false);
        setIsReconnecting(false);
        appendMessage("WebSocket error - Server may be offline", 'system');
      };

      newWS.onclose = (event) => {
        setIsConnected(false);
        setIsReconnecting(false);
        if (event.code !== 1000) {
          appendMessage("Connection lost - Server may be offline", 'system');
        }
      };

      ws.current = newWS;
    } catch (err) {
      appendMessage("Failed to create WebSocket connection", 'system');
    }
  };

  useEffect(() => {
    connectWebSocket();
    return () => {
      ws.current?.close();
    };
  }, []);

  const appendMessage = (content: string, type: 'system' | 'user' | 'server') => {
    const msg: Message = { content, type, timestamp: new Date() };
    setMessages(prev => [...prev, msg]);
  };

  const sendMessage = () => {
    if (ws.current?.readyState === WebSocket.OPEN && inputValue.trim()) {
      ws.current.send(JSON.stringify({ action: "user_input", content: inputValue }));
      appendMessage(inputValue, 'user');
      setInputValue("");
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <Container>
      <Button onClick={connectWebSocket} disabled={isReconnecting}>
        {isReconnecting ? "Connecting..." : "Refresh"}
      </Button>
      <div>Status: {isConnected ? "Connected" : "Disconnected"}</div>
      <MessagesContainer>
        {messages.map((msg, i) => (
          <MessageItem key={i}>
            <MessageHeader type={msg.type}>
              <MessagePrefix type={msg.type}>
                {msg.type === 'system' ? 'SYS' : msg.type === 'user' ? 'YOU' : 'JAVA'}
              </MessagePrefix>
              <MessageTimestamp>{msg.timestamp.toLocaleTimeString()}</MessageTimestamp>
            </MessageHeader>
            <MessageContent>{msg.content}</MessageContent>
          </MessageItem>
        ))}
        <div ref={messagesEndRef} />
      </MessagesContainer>
      <InputContainer>
        <Input
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Enter command..."
          disabled={!isConnected}
        />
        <Button onClick={sendMessage} disabled={!isConnected || !inputValue.trim()}>
          Send
        </Button>
      </InputContainer>
    </Container>
  );
};

export default WebSocketClient;
