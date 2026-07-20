from datetime import datetime
from airflow import DAG
from airflow.operators.python_operator import PythonOperator

import json

def print_hello():
 return 'Hello World'

dag = DAG(
	dag_id='${dagId}',
	description='Hello world example',
	schedule_interval=None,
	start_date=datetime(2017, 3, 20),
	catchup=False
	)

hello_operator = PythonOperator(task_id='hello_task', python_callable=print_hello, dag=dag)

hello_operator
