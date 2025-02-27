"""Email relay logic for the get help feature.

License: BSD-3-Clause
"""
import base64
import json
import os
import smtplib
import email.mime.text
import email.mime.multipart

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
    
    def __init__(self, smtp_host, smtp_port, smtp_user, smtp_pass, destination):
        """Initialize the Config object with SMTP and destination settings.

        Args:
            smtp_host (str): The SMTP host server.
            smtp_port (int): The SMTP port number.
            smtp_user (str): The SMTP username.
            smtp_pass (str): The SMTP password.
            destination (str): The email destination address.
        """
        self._smtp_host = smtp_host
        self._smtp_port = smtp_port
        self._smtp_user = smtp_user
        self._smtp_pass = smtp_pass
        self._destination = destination

    def get_smtp_host(self):
        """Get the SMTP host server.

        Returns:
            str: The SMTP host server.
        """
        return self._smtp_host
    
    def get_smtp_port(self):
        """Get the SMTP port number.

        Returns:
            int: The SMTP port number.
        """
        return self._smtp_port

    def get_smtp_user(self):
        """Get the SMTP username.

        Returns:
            str: The SMTP username.
        """
        return self._smtp_user

    
    def get_smtp_pass(self):
        """Get the SMTP password.

        Returns:
            str: The SMTP password.
        """
        return self._smtp_pass

    def get_destination(self):
        """Get the email destination address.

        Returns:
            str: The email destination address.
        """
        return self._destination


def get_env_var_decrypt(name):
    """Get and decrypt an environment variable.

    Args:
        name: The name of the environment variable.

    Returns:
        Decrypted environment variable value.
    """
    encrypted = os.environ[name]
    decrypted = boto3.client('kms').decrypt(
        CiphertextBlob=base64.b64decode(encrypted),
        EncryptionContext={
            'LambdaFunctionName': os.environ['AWS_LAMBDA_FUNCTION_NAME']
        }
    )['Plaintext'].decode('utf-8')
    return decrypted


def make_message(payload, config):
    
    """
    Create an email message from the payload.

    Args:
        payload: Payload class containing the user's email, description of the
            issue, and simulation code.

    Returns:
        MIMEMultipart: An email message with plaintext component only.
    """
    message = email.mime.multipart.MIMEMultipart()
    message['From'] = config.get_smtp_user()
    message['To'] = config.get_destination()
    message['Subject'] = 'MPS Help Request'

    sender = payload.get_email()
    description = payload.get_description()
    simulation = payload.get_simulation()

    body = BODY_TEMPLATE.format(
        email=sender,
        description=description,
        simulation=simulation
    )

    message.attach(email.mime.text.MIMEText(body, 'plain'))

    return message


def get_payload(data):
    """Convert a parsed JSON payload to a Payload object.

    Returns:
        Parsed payload.
    """
    sender = data['email']
    description = data['description']
    simulation = data['simulation']

    return Payload(sender, description, simulation)


def get_config_from_env():
    smtp_host = get_env_var_decrypt('SMTP_HOST')
    smtp_port = int(get_env_var_decrypt('SMTP_PORT'))
    smtp_user = get_env_var_decrypt('SMTP_USER')
    smtp_pass = get_env_var_decrypt('SMTP_PASSWORD')
    destination = get_env_var_decrypt('DESTINATION')

    return Config(
        smtp_host,
        smtp_port,
        smtp_user,
        smtp_pass,
        destination
    )


def send_message(message, config):
    """
    Send an email message using the specified configuration.

    Args:
        message: The email message to send.
        config: Config class containing the SMTP configuration.
    """
    with smtplib.SMTP(config.get_smtp_host(), config.get_smtp_port()) as server:
        server.starttls()
        server.login(config.get_smtp_user(), config.get_smtp_pass())
        server.send_message(message)


def lambda_handler(event, context):
    """
    AWS Lambda function to handle form submissions from the get_help.js page
    and send emails with the form data.

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

    message = make_message(payload, config)
    send_message(message, config)

    return {
        'statusCode': 200,
        'headers': {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
        },
        'body': json.dumps({'message': 'Success'})
    }
