import { WebPlugin } from '@capacitor/core';

import type { MLKitTextRecognizerPlugin } from './definitions';

export class MLKitTextRecognizerWeb extends WebPlugin implements MLKitTextRecognizerPlugin {
  startRecognizer(): Promise<string> {
    throw new Error('Not implemented on web.');
  }
  
  async echo(): Promise<{ value: string }> {
    throw new Error('Not implemented on web.');
  }

  checkPermissions(): Promise<PermissionStatus> {
    throw new Error('Not implemented on web.');
  }
  requestPermissions(): Promise<PermissionStatus> {
    throw new Error('Not implemented on web.');
  }

}