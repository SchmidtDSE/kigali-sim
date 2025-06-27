/**
 * Simple service worker load script.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API/Using_Service_Workers
 *
 * Adapted from Global Plastics AI Policy Tool by Josh (https://joshsim.org/)
 * and the Global Plastics AI Policy Tool (https://global-plastics-tool.org/)
 * Original under BSD-3-Clause license.
 *
 * @license BSD
 */


const registerServiceWorker = async () => {
  if ("serviceWorker" in navigator) {
    try {
      const registration = await navigator.serviceWorker.register("/service_worker.js", {
        scope: "/",
      });
      registration.update();
    } catch (error) {
      console.error(`Registration failed with ${error}`);
    }
  }
};

registerServiceWorker();
