import React from "react";
import styled from "styled-components";
import WebSocketClient from "./WebSocketClient";

function App() {
  return (
    <Background>
      <WebSocketClient />
    </Background>
  );
}
const Background = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  background: radial-gradient(circle at center, #1a1a2e, #0f0f23);
  min-height: 100vh;
  padding: 2rem;
  overflow: hidden;
`;

export default App;
