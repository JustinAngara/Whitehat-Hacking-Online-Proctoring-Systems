import React from "react";
import styled from "styled-components";

const ProcessManager: React.FC = () => {
  return (
    <Container>
      <h2>Process Manager View</h2>
      <p>This is a placeholder for your process monitoring feature.</p>
    </Container>
  );
};

const Container = styled.div`
  padding: 2rem;
  background: #1a1a2e;
  border-radius: 12px;
  box-shadow: 0 0 20px rgba(100, 255, 218, 0.1);
`;

export default ProcessManager;
