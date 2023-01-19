import type { PermissionState } from '@capacitor/core';
export type CallbackID = string;
export interface RecognizerPermissionStatus {
  camera: PermissionState;
}
export interface RecognizerConfig {
  /**
   * Whether to show plugin logs. If non specified or disabled only errors will be shown
   */
  isLoggingEnabled?: boolean;
  
  /**
   * Barcode scanner configuration
   */
  barcodeScanner: {
    /**
     * If set then barcode scanner will scan for all available types
     */
    allow: boolean;
  };

  /**
   * Text recognizer configuration
   */
  textRecognizer: {
    /**
     * If set then it scans the image for all the text
     */
    allow: boolean;
  };
}

export interface BarCodeRecognizerData {
  content: string;
}

export interface TextRecognizerData {
  content: string;
  /**
   * Only Android versions could tell the confidence percentage of the result
   */
  confidencePercentage?: number;
}

export interface FoundRecognizerCallback {
  barcode?: BarCodeRecognizerData;
  text?: TextRecognizerData;
}

export interface MLKitTextRecognizerPlugin {
  /**
   * Starts the plugin with the passed configuration.
   * Handles the permission checks internally.
   * 
   * The recognizer is not a one time event, it keeps firing the data to you until the user leaves the screen
   * or you cancel the flow.
   *
   * If permission is denied, then a custom exception will be thrown from the client
   *
   * @returns CallbackID to identify the call itself
   */
  startRecognizer(
    config: RecognizerConfig,
    callback: FoundRecognizerCallback,
  ): Promise<CallbackID>;

  killPlugin(callbackId: string): Promise<void>;
}
