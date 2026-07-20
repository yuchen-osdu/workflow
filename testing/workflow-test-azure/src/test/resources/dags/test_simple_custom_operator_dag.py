from airflow import DAG
from airflow.operators.bash_operator import BashOperator
from datetime import datetime, timedelta
from airflow.utils.dates import days_ago
from operators.${operators.simple_custom_operator.name} import HelloOperator

import json

# Following are defaults which can be overridden later on
default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'start_date': days_ago(2),
    'schedule_interval': None,
    'email': ['airflow@example.com'],
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=1),
}

dag = DAG('${dagId}', schedule_interval=None, default_args=default_args)

# t1, t2, t3 and t4 are examples of tasks created using operators

t5 = HelloOperator(task_id='custom-task', name="Kishore", dag=dag)
