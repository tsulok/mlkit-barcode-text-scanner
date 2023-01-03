import { registerPlugin } from '@capacitor/core';

import type { MLKitTextRecognizerPlugin } from './definitions';

const MLKitTextRecognizer = registerPlugin<MLKitTextRecognizerPlugin>('MLKitTextRecognizer', {
  web: () => import('./web').then(m => new m.MLKitTextRecognizerWeb()),
});

export * from './definitions';
export { MLKitTextRecognizer };
