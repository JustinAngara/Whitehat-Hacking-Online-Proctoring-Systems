import React, { useEffect, useRef, useState } from "react";

type Message = string;

const WebSocketClient: React.FC = () => {
  const ws = useRef<WebSocket | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState<string>("");

  useEffect(() => {
    ws.current = new WebSocket("ws://localhost:8080/ws");

    ws.current.onopen = () => {
      console.log("Connected to Java backend");
      appendMessage("Connected to Java WebSocket server");
    };

    ws.current.onmessage = (event) => {
      console.log("Received:", event.data);
      appendMessage(`Java: ${event.data}`);
    };

    ws.current.onerror = (err) => {
      console.error("WebSocket error", err);
    };

    return () => {
      ws.current?.close();
    };
  }, []);

  const appendMessage = (msg: string) => {
    setMessages((prev) => [...prev, msg]);
  };

  const sendMessage = () => {
    if (ws.current?.readyState === WebSocket.OPEN && inputValue.trim() !== "") {
      const payload = { action: "user_input", content: inputValue };
      ws.current.send(JSON.stringify(payload));
      appendMessage(`You: ${inputValue}`);
      setInputValue(""); // clear input field
    }
  };
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div>
      <h2>WebSocket Client (TS)</h2>

      <input
        id="inputField"
        value={inputValue}
        onKeyDown={handleKeyDown}
        onChange={(e) => setInputValue(e.target.value)}
        placeholder="Type a message"
      />
      <button onClick={sendMessage}>Send to Java</button>

      <ul>
        {messages.map((msg, i) => (
          <li key={i}>{msg}</li>
        ))}
      </ul>
    </div>
  );
};

export default WebSocketClient;
