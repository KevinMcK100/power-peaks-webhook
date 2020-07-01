import argparse
import boto3
from botocore.errorfactory import ClientError
import os

# Constants
bucket_name = 'strava-api-jar'
file_name = 'strava-swagger-jar-with-dependencies-1.0.0.jar'

# Get the AWS profile and region passed in as an argument
parser = argparse.ArgumentParser(description='Generates Java JAR for Strava API Library and pushes it to S3.')
parser.add_argument('--profile',
                    required=True,
                    help='Provide an AWS profile to push Strava Swagger JAR to. Specify "default" to use the default AWS profile')
parser.add_argument('--region',
                    required=True,
                    help='Provide the AWS region associated with the AWS profile you want to push to')
args = parser.parse_args()

# Generate Strava API with swagger-codegen
os.system("swagger-codegen generate -i https://developers.strava.com/swagger/swagger.json -l java -o swagger-codegen")

# Add an additional task to the Gradle build file to allow JAR to be created with all dependencies
task = open("fatJarTask.txt", "r")
os.system("cd swagger-codegen && jenv local 1.8 && chmod 777 gradlew")
with open("swagger-codegen/build.gradle", "a") as buildfile:
    buildfile.write(task.read())
os.system("cd swagger-codegen && ./gradlew clean fatJar")

# Push the Strava API JAR to the specified bucket in S3
session = boto3.session.Session(profile_name=args.profile, region_name=args.region)
s3 = session.client('s3')
try:
    s3.head_object(Bucket=bucket_name, Key=file_name)
except ClientError:
    location = {'LocationConstraint': args.region}
    s3.create_bucket(Bucket=bucket_name, CreateBucketConfiguration=location)
response = s3.upload_file("./swagger-codegen/build/libs/{}".format(file_name), bucket_name, file_name)

# Clear down the generated Swagger code
os.system("rm -rf ./swagger-codegen")

# Log success/fail
bucket = s3.list_objects(Bucket=bucket_name, Prefix=file_name)
if bucket:
    print("Successfully created file {} in bucket {} on S3".format(file_name, bucket_name))
else:
    print("Error occurred creating file {} in bucket {} on S3".format(file_name, bucket_name))
