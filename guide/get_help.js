
// URL that will receive the POST request - to be filled in later
const POST_URL = '';

document.addEventListener('DOMContentLoaded', function() {
  // Load the simulation from localStorage and populate the hidden text area
  const savedSimulation = localStorage.getItem('source');
  if (savedSimulation) {
    document.getElementById('simulation').value = savedSimulation;
  }

  // Add event listener to the send button
  const sendButton = document.getElementById('send-button');
  if (sendButton) {
    sendButton.addEventListener('click', function(event) {
      event.preventDefault();
      sendFeedback();
    });
  }
});

/**
 * Sends feedback from the help form to the server
 */
function sendFeedback() {
  const emailElement = document.getElementById('email');
  const descriptionElement = document.getElementById('description');
  const simulationElement = document.getElementById('simulation');
  
  if (!emailElement.value) {
    alert('Please provide your email address so we can contact you.');
    return;
  }
  
  if (!descriptionElement.value.trim()) {
    alert('Please describe how we can help you.');
    return;
  }
  
  // Hide the send button to prevent multiple submissions
  const sendButton = document.getElementById('send-button');
  if (sendButton) {
    sendButton.style.display = 'none';
  }
  
  // Prepare the data to send
  const feedbackData = {
    email: emailElement.value,
    description: descriptionElement.value,
    simulation: simulationElement.value
  };
  
  // Only attempt to POST if there's a URL configured
  if (POST_URL) {
    fetch(POST_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(feedbackData)
    })
    .then(response => {
      if (response.ok) {
        alert('Thank you, someone will be in touch soon.');
      } else {
        throw new Error('Server response was not OK');
      }
    })
    .catch(error => {
      console.error('Error sending message:', error);
      alert('Error sending message. Please try again later.');
      // Show the send button again so they can retry
      if (sendButton) {
        sendButton.style.display = 'inline-block';
      }
    });
  } else {
    // Since POST_URL is empty, simulate a successful submission for now
    alert('Thank you, someone will be in touch soon.');
  }
}
