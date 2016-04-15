from flask import jsonify
from flask_restful import Resource, reqparse
from werkzeug import check_password_hash, generate_password_hash

import databasecontrol
from emailverifier import send_verification_email

""" Define the connection values for the database. """
DBconfig = {
                'HOST' : 'mikereid73.mysql.pythonanywhere-services.com',
                'USER' : 'mikereid73',
                'PASSWORD' : '--------',
                'DATABASE' : 'mikereid73$ProjectDB'
            }

class LogRecord(Resource):
    def post(self):
        parser = reqparse.RequestParser()
        parser.add_argument("guess")
        parser.add_argument("actual")
        parser.add_argument("username")

        args = parser.parse_args()
        guess = args["guess"]
        actual = args["actual"]
        logged_by = args["username"]

        with databasecontrol.usedatabase(DBconfig) as cursor:
            _SQL =  "INSERT INTO Log "
            _SQL += "(guess,actual,logged_by) "
            _SQL += "VALUES (%s,%s,%s)"
            cursor.execute(_SQL, (guess,actual,logged_by,))
            cursor.close()

        with databasecontrol.usedatabase(DBconfig) as cursor:
            _SQL =  "SELECT first_name,vehicle_registration,email "
            _SQL += "FROM MainCarpark "
            _SQL += "WHERE vehicle_registration=%s"
            cursor.execute(_SQL, (actual,))
            data = cursor.fetchone();
            cursor.close()

            if data:
                with databasecontrol.usedatabase(DBconfig) as cursor:
                    _SQL =  "INSERT INTO Infraction "
                    _SQL += "(vehicle_registration,logged_by) "
                    _SQL += "VALUES (%s,%s)"
                    cursor.execute(_SQL, (actual, logged_by,))
                    cursor.close()

                # send email: name, email, reg
                try:
                    name = data[0]
                    reg = data[1]
                    email = data[2]
                    send_verification_email(name, email, reg)
                except Exception:
                    return jsonify(registration=actual, infraction=True)

                return jsonify(registration=actual, infraction=True)
            return jsonify(registration=actual, infraction=False)



class AttemptLogin(Resource):
        def post(self):
            parser = reqparse.RequestParser()
            parser.add_argument("username")
            parser.add_argument("password")

            args = parser.parse_args()
            username = args["username"]
            password = args["password"]

            with databasecontrol.usedatabase(DBconfig) as cursor:
                _SQL =  "SELECT password "
                _SQL += "FROM Staff "
                _SQL += "WHERE username=%s"
                cursor.execute(_SQL, (username,))
                data = cursor.fetchone()
                cursor.close()

                if data and check_password_hash(data[0], password):
                    return jsonify(username=username, success=True)

                return jsonify(username=username, success=False)

class EditPassword(Resource):
    def post(self):
        parser = reqparse.RequestParser()
        parser.add_argument('username')
        parser.add_argument('old_password')
        parser.add_argument('new_password')

        args = parser.parse_args()
        username = args['username']
        old_password = args["old_password"]
        new_password = args["new_password"]

        with databasecontrol.usedatabase(DBconfig) as cursor:
            _SQL =  "SELECT password "
            _SQL += "FROM Staff "
            _SQL += "WHERE username=%s"
            cursor.execute(_SQL, (username,))
            data = cursor.fetchone();
            cursor.close()

            if data and not check_password_hash(data[0], old_password):
                reason = "Username or passsword do not match"
                return jsonify(username=username, success=False, reason=reason)

            if data and check_password_hash(data[0], new_password):
                reason = "New password is the same as the old password"
                return jsonify(username=username, success=False, reason=reason)

        encrypted_password = generate_password_hash(new_password)
        with databasecontrol.usedatabase(DBconfig) as cursor:
            _SQL =  "UPDATE Staff "
            _SQL += "SET password=%s "
            _SQL += "WHERE username=%s"
            cursor.execute(_SQL, (encrypted_password,username,))
            cursor.close()
            return jsonify(username=username, success=True)



