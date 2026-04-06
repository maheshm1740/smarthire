from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    SERVICE_NAME: str = "resume-parser"

    # Kafka (Upstash)
    KAFKA_BOOTSTRAP_SERVERS: str
    KAFKA_SASL_USERNAME: str
    KAFKA_SASL_PASSWORD: str
    KAFKA_CONSUMER_GROUP: str = "resume-parser-group"
    KAFKA_TOPIC_APPLICATION_SUBMITTED: str = "application.submitted"
    KAFKA_TOPIC_RESUME_PARSED: str = "resume.parsed"

    # Cloudflare R2 (S3-compatible)
    R2_ENDPOINT: str
    R2_ACCESS_KEY_ID: str
    R2_SECRET_ACCESS_KEY: str
    R2_BUCKET_NAME: str = "resumes"

    # Anthropic
    ANTHROPIC_API_KEY: str

    # spaCy model
    SPACY_MODEL: str = "en_core_web_sm"


settings = Settings()
