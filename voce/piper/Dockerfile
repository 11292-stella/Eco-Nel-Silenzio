# Server di Piper (sintesi vocale) sempre acceso, con la voce di Riccardo
FROM python:3.12-slim

# Piper con la parte "server" (Flask)
RUN pip install --no-cache-dir "piper-tts[http]"

# Scarica la voce DENTRO l'immagine: il modello non serve su Git,
# chi clona il progetto lo ottiene costruendo l'immagine
WORKDIR /app/voci
RUN python -m piper.download_voices it_IT-riccardo-x_low

EXPOSE 5050

# Come dal terminale, ma dentro il container
CMD ["python", "-m", "piper.http_server", "-m", "it_IT-riccardo-x_low", "--port", "5050"]