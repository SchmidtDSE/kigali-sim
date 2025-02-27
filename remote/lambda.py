
import json
import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

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
    try:
        # Parse the incoming JSON data
        body = event.get('body', '{}')
        if isinstance(body, str):
            data = json.loads(body)
        else:
            data = body
        
        # Extract form information
        email = data.get('email', 'No email provided')
        description = data.get('description', 'No description provided')
        simulation = data.get('simulation', 'No simulation provided')
        
        # Get SMTP configuration from environment variables
        smtp_host = os.environ.get('SMTP_HOST', 'smtp.gmail.com')
        smtp_port = int(os.environ.get('SMTP_PORT', 587))
        smtp_user = os.environ.get('SMTP_USER')
        smtp_pass = os.environ.get('SMTP_PASSWORD')
        
        if not smtp_user or not smtp_pass:
            raise ValueError("SMTP credentials not configured")
        
        # Create the email message
        msg = MIMEMultipart()
        msg['From'] = smtp_user
        msg['To'] = 'hello@mlf-policy-explorer.org'
        msg['Subject'] = 'Montreal Policy Simulator Help Request'
        
        # Create the email body
        body = f"""
        A user has submitted a help request from the Montreal Policy Simulator:
        
        User email: {email}
        
        Description of issue:
        {description}
        
        ----------------------------------------
        Simulation code:
        ----------------------------------------
        {simulation}
        ----------------------------------------
        """
        
        msg.attach(MIMEText(body, 'plain'))
        
        # Send the email
        with smtplib.SMTP(smtp_host, smtp_port) as server:
            server.starttls()
            server.login(smtp_user, smtp_pass)
            server.send_message(msg)
        
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            'body': json.dumps({'message': 'Email sent successfully'})
        }
        
    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            'body': json.dumps({'message': f'Error sending email: {str(e)}'})
        }
