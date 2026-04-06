from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    SERVICE_NAME: str = "jd-generator"

    # Kafka
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_SASL_USERNAME: str = ""
    KAFKA_SASL_PASSWORD: str = ""
    KAFKA_CONSUMER_GROUP: str = "jd-generator-group"
    KAFKA_TOPIC_JOB_CREATED: str = "job.created"

    # Anthropic
    ANTHROPIC_API_KEY: str = "sk-ant-test"

    # Claude model
    CLAUDE_MODEL: str = "claude-sonnet-4-20250514"
    CLAUDE_MAX_TOKENS: int = 1500


settings = Settings()
