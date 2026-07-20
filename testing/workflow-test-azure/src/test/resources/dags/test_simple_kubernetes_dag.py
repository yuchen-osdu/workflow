from airflow import DAG
from datetime import datetime, timedelta
from airflow.contrib.operators.kubernetes_pod_operator import KubernetesPodOperator
from airflow.operators.dummy_operator import DummyOperator
from airflow import configuration as conf
from airflow.utils.dates import days_ago

import json

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'email': ['username@example.com'],
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=1)
}

dag = DAG(
    '${dagId}',
    schedule_interval=None,
    default_args=default_args,
    start_date=days_ago(2))

start = DummyOperator(task_id='run_this_first', dag=dag)
passing = KubernetesPodOperator(namespace="airflow",
                          image="python:3.6",
                          cmds=["python","-c"],
                          arguments=["print('hello world')"],
                          labels={"foo": "bar"},
                          name="passing-test",
                          task_id="passing-task",
                          get_logs=True,
                          in_cluster=True,
                          is_delete_operator_pod=False,
                          dag=dag
                          )

start >> passing
