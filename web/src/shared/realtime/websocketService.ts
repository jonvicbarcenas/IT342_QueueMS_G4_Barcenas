import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import type { Client, Subscription } from 'stompjs';
import { api } from '@/shared/api/api';

class WebSocketService {
  private client: Client | null = null;
  private connected = false;
  private connecting: Promise<void> | null = null;
  private connectionAttempt = 0;
  private subscriptions: Map<string, Subscription> = new Map();

  /**
   * Connect to the WebSocket server
   */
  public connect(): Promise<void> {
    if (this.connected) {
      return Promise.resolve();
    }

    if (this.connecting) {
      return this.connecting;
    }

    const attempt = ++this.connectionAttempt;
    this.connecting = new Promise((resolve, reject) => {
      const socket = new SockJS(`${api.API_BASE_URL}/ws`);
      this.client = Stomp.over(socket);
      
      // Disable logging in production
      if (import.meta.env.PROD) {
        this.client.debug = () => {};
      }

      this.client.connect(
        {},
        () => {
          if (attempt !== this.connectionAttempt) {
            resolve();
            return;
          }

          this.connected = true;
          this.connecting = null;
          resolve();
        },
        (error) => {
          if (attempt !== this.connectionAttempt) {
            resolve();
            return;
          }

          this.connected = false;
          this.connecting = null;
          reject(error);
        }
      );
    });

    return this.connecting;
  }

  /**
   * Subscribe to a topic
   */
  public subscribe<TMessage = unknown>(
    topic: string,
    callback: (message: TMessage) => void
  ): Subscription | null {
    if (!this.client || !this.connected) {
      console.warn('WebSocket not connected. Unable to subscribe.');
      return null;
    }

    const subscription = this.client.subscribe(topic, (message) => {
      try {
        callback(JSON.parse(message.body) as TMessage);
      } catch (error) {
        console.error('Failed to handle WebSocket message:', error);
      }
    });

    this.subscriptions.set(topic, subscription);
    return subscription;
  }

  /**
   * Unsubscribe from a topic
   */
  public unsubscribe(topic: string): void {
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(topic);
    }
  }

  /**
   * Disconnect from the WebSocket server
   */
  public disconnect(): void {
    this.connectionAttempt += 1;
    this.connecting = null;
    this.subscriptions.clear();

    if (!this.client) {
      this.connected = false;
      return;
    }

    if (this.connected) {
      this.client.disconnect(() => {
        this.connected = false;
        this.client = null;
      });
      return;
    }

    this.connected = false;

    try {
      this.client.ws?.close();
    } catch (error) {
      console.warn('Unable to close WebSocket connection:', error);
    }

    this.client = null;
  }

  public isConnected(): boolean {
    return this.connected;
  }
}

export const webSocketService = new WebSocketService();
