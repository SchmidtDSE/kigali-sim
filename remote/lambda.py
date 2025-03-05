"""Email relay logic for the get help feature.

License: BSD-3-Clause
"""
import json
import os

import boto3

BODY_TEMPLATE = """
Hello!

A user has submitted a help request from the Montreal Policy Simulator.

Thanks,
HelpBot


User email: {email}

Description of issue:
{description}

----------------------------------------
Simulation code:
----------------------------------------
{simulation}
----------------------------------------
"""


class Payload:
    """Represents the payload data for the help request email. """

    def __init__(self, email, description, simulation):
        """Initialize the Payload object with email, description, and simulation.

        Args:
            email (str): The user's email address.
            description (str): The description of the issue.
            simulation (str): The simulation code related to the issue.
        """
        self._email = email
        self._description = description
        self._simulation = simulation

    def get_email(self):
        """Get the user's email address.

        Returns:
            str: The user's email address.
        """
        return self._email

    def get_description(self):
        """Get the description of the issue.

        Returns:
            str: The description of the issue.
        """
        return self._description

    def get_simulation(self):
        """Get the simulation code.

        Returns:
            str: The simulation code.
        """
        return self._simulation


class Config:
    """Represents the configuration for sending emails."""

    def __init__(self, from_email, to_email, subject):
        """Initialize the Config object with message metadata settings.

        Args:
            from_email (str): The email from which the message should be sent.
            to_email (int): The email to which the message should be sent.
            subject (str): The subject line for the message.
        """
        self._from_email = from_email
        self._to_email = to_email
        self._subject = subject

    def get_from_email(self):
        """Get the from email address.

        Returns:
            str: The from email address also used for return email.
        """
        return self._from_email

    def get_to_email(self):
        """Get the to email address.

        Returns:
            int: The to email address (recipient of bug report).
        """
        return self._to_email

    def get_subject(self):
        """Get the subject line for the message.

        Returns:
            str: The subject line for the message (plaintext).
        """
        return self._subject


def make_message(payload):
    """Create an email message from the payload.

    Args:
        payload (Payload): Payload class containing the user's email,
            description of the issue, and simulation code.

    Returns:
        str: Message body rendered as a string.
    """
    sender = payload.get_email()
    description = payload.get_description()
    simulation = payload.get_simulation()

    return BODY_TEMPLATE.format(
        email=sender,
        description=description,
        simulation=simulation
    )


def get_payload(data):
    """Convert a parsed JSON payload to a Payload object.

    Returns:
        Payload: Parsed payload.
    """
    sender = data['email']
    description = data['description']
    simulation = data['simulation']

    return Payload(sender, description, simulation)


def get_config_from_env():
    """Retrieve configuration from environment variables.

    Returns:
        Config: The configuration object containing metadata settings including
            sender and recipient addresses.
    """
    from_email = os.getenv('HELP_EMAIL_FROM')
    to_email = os.getenv('HELP_EMAIL_TO')
    subject = os.getenv('HELP_EMAIL_SUBJECT')

    return Config(from_email, to_email, subject)


def send_message(message, config):
    """Send an email message using the specified configuration.

    Args:
        message (str): The email message to send as a string.
        config (Config): Config class containing the SMTP configuration.
    """
    ses = boto3.client('ses')

    from_email = config.get_from_email()
    to_email = config.get_to_email()
    subject = config.get_subject()

    response = ses.send_email(
        Source=from_email,
        Destination={
            'ToAddresses': [to_email]
        },
        Message={
            'Subject': {
                'Data': subject
            },
            'Body': {
                'Text': {
                    'Data': message
                }
            }
        }
    )

    if response['ResponseMetadata']['HTTPStatusCode'] != 200:
        raise RuntimeError('Non-OK response from SES.')


def lambda_handler(event, context):
    """Send emails on behalf of get_help.html

    Args:
        event: The event data containing the HTTP request details
        context: The Lambda execution context

    Returns:
        A response object indicating success or failure
    """
    body = event.get('body', '{}')
    data = json.loads(body)

    payload = get_payload(data)
    config = get_config_from_env()

    message = make_message(payload)
    send_message(message, config)

    return {
        'statusCode': 200,
        'headers': {
            'Content-Type': 'application/json'
        },
        'body': json.dumps({'message': 'Success'})
    }
