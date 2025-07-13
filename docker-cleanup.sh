#!/bin/bash
# Remove all exited containers
docker rm $(docker ps -a -q -f status=exited)

# Remove all dangling images (untagged)
docker rmi $(docker images -f "dangling=true" -q)

# Optionally, remove your app image (uncomment if needed)
# docker rmi todo-javamongo

echo "Cleanup complete."
