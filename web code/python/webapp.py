from flask import Flask, render_template, request, redirect, session
from flask import jsonify
from flask_restful import Resource, Api, reqparse
from functools import wraps
from werkzeug import check_password_hash, generate_password_hash

from mobile_api import LogRecord, AttemptLogin, EditPassword

import databasecontrol

""" Define the connection values for the database. """
DBconfig = {
                'HOST' : 'mikereid73.mysql.pythonanywhere-services.com',
                'USER' : 'mikereid73',
                'PASSWORD' : 'alongpassword',
                'DATABASE' : 'mikereid73$ProjectDB'
            }

app = Flask(__name__)
app.secret_key = '101010'
api = Api(app)

api.add_resource(LogRecord, '/post')
api.add_resource(AttemptLogin, '/login')
api.add_resource(EditPassword, '/change_password')

def check_is_logged_in(func):
    @wraps(func)
    def wrapped_function(*args, **kwargs):
        if session.get('logged_in') == True:
            return func(*args, **kwargs)
        return 'Please log in to view this page.'
    return wrapped_function

@app.route('/login')
def display_login_page() -> 'html':
    file = 'login.html'
    return render_template(file, the_title = 'Log In')

def authenticate_user(username, password):
    with databasecontrol.usedatabase(DBconfig) as cursor:
        _SQL = "SELECT password FROM AdminStaff WHERE username=%s"
        cursor.execute(_SQL, (username,))
        data = cursor.fetchone()
        cursor.close()
        if data:
            encrypted_password = data[0]
            return check_password_hash(encrypted_password, password)
    return False

""" Attempts to log the user in. """
@app.route('/attempt_login', methods=['POST'])
def process_login() -> 'html':
    username = request.form['username']
    password = request.form['password']

    # Check if username/password supplied match those in DB.
    credential_match = authenticate_user(username, password)

    if credential_match:
        session['logged_in'] = True
        return redirect('/main_menu')
    return 'Username or password is incorrect.'

""" Direct the user to the logout page. """
""" Can't go here if not logged in. """
@app.route('/logout')
@check_is_logged_in
def display_logout_page() -> 'html':
    file = 'logout.html'
    return render_template(file, the_title='Log Out')

""" Attempts to log the user out. """
@app.route('/attempt_logout', methods=['POST'])
def process_logout() -> 'html':
    # Clear stored information from session
    session.clear()
    return redirect('/login')

@app.route('/')
def default_page():
    if session.get('logged_in') == True:
        return redirect('/main_menu')
    return redirect('/login')

@app.route('/main_menu')
@check_is_logged_in
def main_page():
    file = 'main_menu.html'
    return render_template(file, the_title = 'Dashboard')

@app.route('/register_new_vehicle')
@check_is_logged_in
def display_register_vehicle_page() -> 'html':
    file = 'register_vehicle.html'
    return render_template(file, the_title = 'Register New Vehicle')

@app.route('/attempt_register_vehicle', methods=['POST'])
@check_is_logged_in
def process_register_vehicle() -> 'html':
    first_name = request.form['first_name']
    last_name = request.form['last_name']
    vehicle_registration = request.form['vehicle_registration']
    email = request.form['email']

    with databasecontrol.usedatabase(DBconfig) as cursor:
        _SQL =  "INSERT INTO MainCarpark "
        _SQL += "(last_name,first_name,vehicle_registration,email) "
        _SQL += "VALUES (%s,%s,%s,%s)"
        cursor.execute(_SQL, (last_name,first_name,vehicle_registration,email,))
        cursor.close()

    return render_template('register_vehicle_verify.html',
                            first_name=first_name,
                            last_name=last_name,
                            vehicle_registration=vehicle_registration,
                            email=email,
                            the_title='Register Vehicle Success')

@app.route('/view_all_vehicles', methods=['GET'])
@check_is_logged_in
def display_all_registered_vehicles() -> 'html':
    with databasecontrol.usedatabase(DBconfig) as cursor:
        _SQL =  "Select first_name,last_name,vehicle_registration,email "
        _SQL += "FROM MainCarpark "
        _SQL += "ORDER by last_name"
        cursor.execute(_SQL,)
        all_vehicles = cursor.fetchall()
        cursor.close()

    return render_template('view_all_vehicles.html',
                            all_vehicles=all_vehicles,
                            the_title='View All Registered Vehicles')

@app.route('/register_new_staff')
@check_is_logged_in
def display_register_page() -> 'html':
    file = 'register_staff.html'
    return render_template(file, the_title = 'Register')

@app.route('/attempt_register', methods=['POST'])
@check_is_logged_in
def process_register() -> 'html':
    username = request.form['username']
    password = request.form['password']
    email = request.form['email']

    encrypted_password = generate_password_hash(password)
    with databasecontrol.usedatabase(DBconfig) as cursor:
        _SQL =  "INSERT INTO Staff "
        _SQL += "(username, password,email) "
        _SQL += "VALUES (%s,%s,%s)"
        cursor.execute(_SQL, (username, encrypted_password,email,))
        cursor.close()

    return render_template('register_staff_verify.html',
                            username=username,
                            email=email,
                            the_title='Register Staff Success')

@app.route('/view_all_infractions', methods=['GET'])
@check_is_logged_in
def getAllInfractions():
    with databasecontrol.usedatabase(DBconfig) as cursor:
        _SQL =  "SELECT Infraction.vehicle_registration,last_name,first_name,email,time,logged_by "
        _SQL += "FROM Infraction "
        _SQL += "INNER JOIN MainCarpark "
        _SQL += "ON Infraction.vehicle_registration = MainCarpark.vehicle_registration "
        _SQL += "ORDER by Infraction.vehicle_registration, time"
        cursor.execute(_SQL,)
        all_data = cursor.fetchall()
        cursor.close()

    with databasecontrol.usedatabase(DBconfig) as cursor:
        _SQL =  "SELECT Infraction.vehicle_registration,last_name,first_name,email,time,logged_by "
        _SQL += "FROM Infraction "
        _SQL += "INNER JOIN MainCarpark "
        _SQL += "ON Infraction.vehicle_registration = MainCarpark.vehicle_registration "
        _SQL += "WHERE time >= CURDATE() "
        _SQL += "ORDER by vehicle_registration, time"
        cursor.execute(_SQL,)
        today_data = cursor.fetchall()
        cursor.close()

    with databasecontrol.usedatabase(DBconfig) as cursor:
        _SQL =  "SELECT Infraction.vehicle_registration,last_name,first_name,email,time,logged_by "
        _SQL += "FROM Infraction "
        _SQL += "INNER JOIN MainCarpark "
        _SQL += "ON Infraction.vehicle_registration = MainCarpark.vehicle_registration "
        _SQL += "WHERE time >= DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE())-1 DAY) "
        _SQL += "ORDER by vehicle_registration, time"
        cursor.execute(_SQL,)
        month_data = cursor.fetchall()
        cursor.close()

    with databasecontrol.usedatabase(DBconfig) as cursor:
        _SQL =  "SELECT Infraction.vehicle_registration,last_name,first_name,email,COUNT(Infraction.vehicle_registration) "
        _SQL += "FROM Infraction,MainCarpark "
        _SQL += "WHERE Infraction.vehicle_registration=MainCarpark.vehicle_registration "
        _SQL += "GROUP by Infraction.vehicle_registration "
        _SQL += "HAVING COUNT(Infraction.vehicle_registration) > 1 "
        _SQL += "ORDER by COUNT(Infraction.vehicle_registration)"

        cursor.execute(_SQL,)
        repeat_data = cursor.fetchall()
        cursor.close()

    return render_template('view_infractions.html',
                            all_data=all_data,
                            today_data=today_data,
                            month_data=month_data,
                            repeat_data=repeat_data,
                            the_title='All Logged Infractions')

if __name__ == '__main__':
    app.run(debug=True)