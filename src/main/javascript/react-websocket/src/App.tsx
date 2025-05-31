import WebSocketClient from "./WebSocketClient";

function App() {
  return (
    <div style={{ padding: "1rem" }}>
      <h1>Welcome, type anything to interact with the Java Application</h1>
      <WebSocketClient />
    </div>
  );
}

export default App;
