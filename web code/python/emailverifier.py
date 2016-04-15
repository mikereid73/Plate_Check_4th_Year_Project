import smtplib

from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

def send_verification_email(name, email, reg):
    message = MIMEMultipart('alternative')
    html = """  <html>
                    <head>
                    <b><u>Important!</u></b><br><br>
                    </head>
                    <body>
                        """ + name + """,<br>

                        Your vehicle with registration """ + reg + """ has
                        been recorded as parking in the visitors carpark.

                        Please move to the main carpark as soon as possible.<br>

                        <br>Thanks,<br>
                        <b>Carlow IT Admin<br>
                    </body>
                </html>   """

    content = MIMEText(html, 'html')
    message.attach(content)
    message['Subject'] = 'Vehicle Reg: ' + reg

    mail = smtplib.SMTP('smtp.gmail.com',587)
    mail.ehlo()
    mail.starttls()
    mail.login('webappc00112726@gmail.com','random333')
    mail.sendmail('administrator.webappc00112726@gmail.com', email, message.as_string())
    mail.close()

    if __name__ == '__main__':
        send_verification_email()

