import { onMounted, onBeforeUnmount } from 'vue';

export function useWebSocket(onMessageReceived: (message: string) => void) {
  let socket: WebSocket | null = null;

  function connect(url: string) {
    socket = new WebSocket(url);

    socket.onmessage = (event: MessageEvent) => {
      const message = event.data;
      onMessageReceived(message); // Callback - Llama al método proporcionado al recibir un mensaje
    };

    socket.onopen = () => {
      console.log('WebSocket connected');
    };

    socket.onclose = () => {
      console.log('WebSocket disconnected');
    };

    socket.onerror = (error: Event) => {
      console.error('WebSocket error:', error);
    };
  }

  function disconnect() {
    if (socket) {
      socket.close();
    }
  };

  return {
    connect,
    disconnect
  }
}
