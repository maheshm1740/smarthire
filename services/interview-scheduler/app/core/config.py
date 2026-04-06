from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    SERVICE_NAME: str = "interview-scheduler"

    # Kafka
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_SASL_USERNAME: str = ""
    KAFKA_SASL_PASSWORD: str = ""
    KAFKA_CONSUMER_GROUP: str = "interview-scheduler-group"
    KAFKA_TOPIC_INTERVIEW_SCHEDULED: str = "interview.scheduled"
    KAFKA_TOPIC_INTERVIEW_CANCELLED: str = "interview.cancelled"
    KAFKA_TOPIC_INTERVIEW_RESCHEDULED: str = "interview.rescheduled"

    # MongoDB
    MONGO_URI: str = "mongodb://localhost:27017"
    MONGO_DB: str = "smarthire"
    MONGO_COLLECTION_INTERVIEWS: str = "interviews"

    # Google OAuth2 / Calendar
    GOOGLE_CLIENT_ID: str = ""
    GOOGLE_CLIENT_SECRET: str = ""
    GOOGLE_REDIRECT_URI: str = "http://localhost:8092/auth/google/callback"
    GOOGLE_TOKEN_FILE: str = "token.json"   # local file to persist OAuth2 tokens


settings = Settings()
