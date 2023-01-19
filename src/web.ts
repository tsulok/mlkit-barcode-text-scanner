import { WebPlugin } from '@capacitor/core';

import type { MLKitTextRecognizerPlugin } from './definitions';

export class MLKitTextRecognizerWeb extends WebPlugin implements MLKitTextRecognizerPlugin {
  killPlugin(_: string): Promise<void> {
    throw new Error('Method not implemented.');
  }
  startRecognizer(): Promise<string> {
    throw new Error('Not implemented on web.');
  }
}