/**
 * Logic to run the get help page.
 *
 * @license BSD, see LICENSE.md.
 */

const POST_URL = "https://uyppqr75wwrl3uejzvbm5c3qh40aoncr.lambda-url.us-east-2.on.aws/";
const FAIL_MESSAGE = "Sorry we ran into an issue. Please email hello@mlf-policy-explorer.org.";

/**
 * Check if a given string is empty.
 * 
 * @param {string} testInput - The string to check.
 * @returns {boolean} True if the string is empty after removing starting / ending whitespace
 *    and false otherwise.
 */
function isEmpty(testInput) {
  const trimmed = testInput.trim();
  return trimmed === "";
}

/**
 * Validates the email and description inputs from the help form.
 *
 * @param {string} email - Email address provided by the user.
 * @param {string} description - Description provided by the user.
 * @returns {boolean} - Returns true if both inputs are valid, otherwise false.
 */
function checkInputs(email, description) {
  if (isEmpty(email)) {
    alert("Please provide your email address so we can contact you.");
    return false;
  }

  if (isEmpty(description)) {
    alert("Please describe how we can help you.");
    return false;
  }

  return true;
}

/**
 * Hide the send button.
 *
 * Hides the send button after the feedback form has been submitted
 * to prevent multiple submissions.
 */
function hideSend() {
  const sendButton = document.getElementById("send-button");
  sendButton.style.display = "none";

  const sendingMessage = document.getElementById("sending-message");
  sendingMessage.style.display = "block";
}

/**
 * Shows the confirmation message after successfully sending feedback.
 *
 * Hides the sending message and displays the sent confirmation message
 * to inform the user that their feedback has been successfully sent.
 */
function showConfirmation() {
  const sendingMessage = document.getElementById("sending-message");
  sendingMessage.style.display = "none";

  const sentMessage = document.getElementById("sent-message");
  sentMessage.style.display = "block";
}

/**
 * Sends feedback from the help form to the server.
 */
function sendFeedback() {
  const emailElement = document.getElementById("email");
  const descriptionElement = document.getElementById("description");
  const simulationElement = document.getElementById("simulation");

  const email = emailElement.value;
  const description = descriptionElement.value;
  const simulation = simulationElement.value;

  if (!checkInputs(email, description)) {
    return;
  }

  hideSend();

  const feedbackData = {
    email: email,
    description: description,
    simulation: simulation,
  };

  const postAction = fetch(POST_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(feedbackData),
  });

  const postHandler = postAction.then((response) => {
    if (response.ok) {
      showConfirmation();
    } else {
      alert(FAIL_MESSAGE);
    }
  });

  postHandler.catch((error) => {
    alert(FAIL_MESSAGE);
  });
}

/**
 * Get the code from HTML5 local storage.
 *
 * Get the current simulation code that the user is working on which is
 * persisted to local storage which is a limited space area on disk
 * which is autosaved as the user works. Local storage is persisted between
 * browser sessions.
 *
 * @returns {string} The code that the user was working on and which was
 *    last autosaved to local storage.
 */
function getCurrentCode() {
  const savedSimulation = localStorage.getItem("source");
  return savedSimulation;
}

/**
 * Main function to initialize the get help page logic.
 *
 * Loads any saved simulation data from localStorage into the input fields
 * and sets up the event listener for the send button.
 */
function main() {
  const savedSimulation = getCurrentCode();
  if (savedSimulation) {
    document.getElementById("simulation").value = savedSimulation;
  }

  const sendButton = document.getElementById("send-button");
  sendButton.addEventListener("click", function (event) {
    event.preventDefault();
    sendFeedback();
  });
}

main();
