import { SplashScreen } from '@capacitor/splash-screen';
import { MLKitTextRecognizer } from "mlkit-text-barcode-scanner";

window.customElements.define(
  'capacitor-welcome',
  class extends HTMLElement {

    savedCallbackId = undefined;

    constructor() {
      super();

      SplashScreen.hide();

      const root = this.attachShadow({ mode: 'open' });

      root.innerHTML = `
    <style>
      :host {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
        display: block;
        width: 100%;
        height: 100%;
      }
      h1, h2, h3, h4, h5 {
        text-transform: uppercase;
      }
      .button {
        display: inline-block;
        padding: 10px;
        background-color: #73B5F6;
        color: #fff;
        font-size: 0.9em;
        border: 0;
        border-radius: 3px;
        text-decoration: none;
        cursor: pointer;
      }
      main {
        padding: 15px;
      }
      main hr { height: 1px; background-color: #eee; border: 0; }
      main h1 {
        font-size: 1.4em;
        text-transform: uppercase;
        letter-spacing: 1px;
      }
      main h2 {
        font-size: 1.1em;
      }
      main h3 {
        font-size: 0.9em;
      }
      main p {
        color: #333;
      }
      main pre {
        white-space: pre-line;
      }
    </style>
    <div>
      <capacitor-welcome-titlebar>
        <h1>Capacitor</h1>
      </capacitor-welcome-titlebar>
      <main>
        <p>
          Capacitor makes it easy to build powerful apps for the app stores, mobile web (Progressive Web Apps), and desktop, all
          with a single code base.
        </p>
        <h2>Getting Started</h2>
        <p>
          You'll probably need a UI framework to build a full-featured app. Might we recommend
          <a target="_blank" href="http://ionicframework.com/">Ionic</a>?
        </p>
        <p>
          Visit <a href="https://capacitorjs.com">capacitorjs.com</a> for information
          on using native features, building plugins, and more.
        </p>
        <a href="https://capacitorjs.com" target="_blank" class="button">Read more</a>
        <h2>Tiny Demo</h2>
        <p>
          This demo shows how to call Capacitor plugins. Say cheese!
        </p>
        <p>
          <button class="button" id="take-photo">Take Photo</button>
        </p>
        <p>
          <img id="image" style="max-width: 100%">
        </p>
      </main>
    </div>
    `;
    }

    connectedCallback() {
      const self = this;

      self.shadowRoot.querySelector('#take-photo').addEventListener('click', async function (e) {
        try {
          console.log("Starting recognizer");
          MLKitTextRecognizer.startRecognizer({
            config: {
              isLoggingEnabled: true,
              barcodeScanner: { allow: true },
              textRecognizer: { allow: false },
            },
          }, (callbackData) => {
            if (callbackData.text) {
              console.log(`Recognized text: ${callbackData.text?.content}`)
              console.log(`Confidence: ${callbackData.text?.confidencePercentage}`)
            }

            if (callbackData.barcode) {
              console.log(`Recognized barcode: ${callbackData.barcode?.content}`)

              if (callbackData.barcode?.content === "12345670") {
                console.log("Invoking plugin kill from JS");
                MLKitTextRecognizer.killPlugin(this.savedCallbackId).then(() => {
                  console.log("Plugin is killed");
                });
              }
            }
          }).then(
            (callbackId) => {
              console.log(`Callback id received ${callbackId}`);
              this.savedCallbackId = callbackId
            },
            (error) => {
              console.error("Something bad happened");
              console.error(error);
            }
          );
        } catch (e) {
          console.warn("Something bad happened", e);
        }
      });
    }
  }
);

window.customElements.define(
  'capacitor-welcome-titlebar',
  class extends HTMLElement {
    constructor() {
      super();
      const root = this.attachShadow({ mode: 'open' });
      root.innerHTML = `
    <style>
      :host {
        position: relative;
        display: block;
        padding: 15px 15px 15px 15px;
        text-align: center;
        background-color: #73B5F6;
      }
      ::slotted(h1) {
        margin: 0;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
        font-size: 0.9em;
        font-weight: 600;
        color: #fff;
      }
    </style>
    <slot></slot>
    `;
    }
  }
);
