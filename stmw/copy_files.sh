#!/bin/bash

# Check if the student workspace is empty and copy skeleton files if needed
if [ ! -f "/student_workspace/MyDOM.java" ]; then
    echo "Copying MyDOM.java to the student workspace..."
    cp /stmw/skeleton_files/MyDOM.java /stmw/student_workspace/
fi

if [ ! -f "/student_workspace/MySAX.java" ]; then
    echo "Copying MySAX.java to the student workspace..."
    cp /stmw/skeleton_files/MySAX.java /stmw/student_workspace/
fi

if [ ! -f "/student_workspace/runLoad.sh" ]; then
    echo "Copying runLoad.sh to the student workspace..."
    cp /stmw/skeleton_files/runLoad.sh /stmw/student_workspace/
fi

echo "Welcome to Programming Assignment 1! \n\n"

# Start the bash shell
exec "$@"