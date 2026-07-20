FROM google/cloud-sdk:alpine

WORKDIR /opt

COPY ./provider/workflow-gc/bootstrap/ /opt/

RUN apk add jq
RUN chmod +x bootstrap.sh validate-env.sh

RUN addgroup -g 10001 -S nonroot \
  && adduser -h /opt -G nonroot -S -u 10001 nonroot
RUN chown -R 10001:10001 /opt
USER 10001:10001

CMD ["/bin/bash", "-c", "./bootstrap.sh && sleep 365d"]
