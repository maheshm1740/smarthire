from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    SERVICE_NAME: str = "ranking-engine"

    # Kafka (Upstash or local Docker)
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_SASL_USERNAME: str = ""
    KAFKA_SASL_PASSWORD: str = ""
    KAFKA_CONSUMER_GROUP: str = "ranking-engine-group"
    KAFKA_TOPIC_RESUME_PARSED: str = "resume.parsed"
    KAFKA_TOPIC_CANDIDATE_RANKED: str = "candidate.ranked"

    # MongoDB Atlas or local Docker
    MONGO_URI: str = "mongodb://localhost:27017"
    MONGO_DB: str = "smarthire"
    MONGO_COLLECTION_CANDIDATES: str = "candidates"
    MONGO_COLLECTION_JOBS: str = "jobs"

    # Sentence-transformers model
    EMBEDDING_MODEL: str = "all-MiniLM-L6-v2"

    # Scoring weights (must sum to 1.0)
    WEIGHT_SKILLS: float = 0.40
    WEIGHT_EXPERIENCE: float = 0.30
    WEIGHT_EDUCATION: float = 0.20
    WEIGHT_SEMANTIC: float = 0.10


settings = Settings()