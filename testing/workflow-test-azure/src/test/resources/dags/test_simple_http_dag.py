import json
from datetime import timedelta

from airflow import DAG
from airflow.operators.http_operator import SimpleHttpOperator
from airflow.utils.dates import days_ago

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'email': ['airflow@example.com'],
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=1),
}

dag = DAG('${dagId}', schedule_interval=None, default_args=default_args, tags=['example'], start_date=days_ago(2))

dag.doc_md = __doc__

task_get_op = SimpleHttpOperator(
    task_id='get_op',
    method='GET',
    endpoint='get',
    data={"param1": "value1", "param2": "value2"},
    headers={},
    dag=dag,
)

task_get_op
