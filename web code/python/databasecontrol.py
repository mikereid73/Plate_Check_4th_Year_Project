import mysql.connector

class usedatabase:
	def __init__(self, configuration:dict):
		self.host = configuration['HOST']
		self.user = configuration['USER']
		self.password = configuration['PASSWORD']
		self.database = configuration['DATABASE']

	def __enter__(self):
		self.conn = mysql.connector.connect( host = self.host,
		                                    user = self.user,
											password = self.password,
											database = self.database)
		self.cursor = self.conn.cursor()
		return self.cursor

	def __exit__(self, exc_type, exc_value,  exc_traceback):
		self.cursor.close()
		self.conn.commit()
		self.conn.close()