import React from "react";
import styled from "styled-components";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import WebSocketClient from "./WebSocketClient";
import ProcessManager from "./ProcessManager";
import Navbar from "./Navbar";

function App() {
  return (
    <Router>
      <Background>
        <Navbar />
        <Content>
          <Routes>
            <Route path="/" element={<WebSocketClient />} />
            <Route path="/process" element={<ProcessManager />} />
          </Routes>
        </Content>
      </Background>
    </Router>
  );
}
const Background = styled.div`
  display: flex;
  flex-direction: row;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #121212, #1f1f3f);
  color: #e0e6ed;
  overflow: hidden;
`;


const Content = styled.main`
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
`;


export default App;
