FROM ubuntu:24.04

RUN apt update && \
    apt install -y g++ cmake make build-essential && \
    apt clean

WORKDIR /app

COPY . .

RUN mkdir build && cd build && cmake .. && make -j$(nproc)

EXPOSE 8884

CMD ["./build/cpp_sandbox"]
