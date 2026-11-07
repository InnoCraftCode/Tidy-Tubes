"""Renders the trailer narration with Kokoro (kokoro-onnx, Apache 2.0, runs offline).

Usage (the venv and model are set up once, see play-store/<game>/video/trailer/README.md):
    <venv>\\Scripts\\python.exe -I voice.py <model dir> <out dir> en|fr

Writes <out dir>/<lang>/<id>.wav (24 kHz mono, leading/trailing silence trimmed) and
durations.txt ("id seconds" per line), which TrailerComposer reads to place the lines.
"""
import os
import sys

import numpy as np
import soundfile as sf
from kokoro_onnx import Kokoro

# "Tidy Tubes" is an English name: the French voice gets a phonetic spelling of it.
LINES = {
    'en': {
        'voice': 'af_heart', 'lang': 'en-us', 'speed': 0.92,
        'lines': {
            'm01': 'Somewhere, the colours are all mixed up.',
            'm02': 'Tangled. Restless. Out of place.',
            'm03': 'It only takes one move...',
            'm04': 'to set things right.',
            'm05': 'Tidy Tubes.',
            'm06': 'Pick a ball. Drop it home. One colour per tube.',
            'm07': 'Endless levels. Up to ten colours. Every one solvable.',
            'm08': 'Stuck? Ask for a hint from a real solver.',
            'm09': 'Unlock four hundred styles. Balls, tubes, finishes and themes.',
            'm10': 'Every star you earn moves you along the star road.',
            'm11': 'Calm music. Eight languages. Play offline, anytime.',
            'm12': 'Tidy Tubes. Sort. Breathe. Repeat.',
            'm13': 'Free on Google Play.',
            's01': 'Colours out of place?',
            's02': 'Sort them. One tube at a time.',
            's03': 'Endless levels. Four hundred styles.',
            's04': 'Tidy Tubes. Free on Google Play.',
        },
    },
    'fr': {
        'voice': 'ff_siwis', 'lang': 'fr-fr', 'speed': 0.95,
        'lines': {
            'm01': 'Quelque part, les couleurs sont toutes mélangées.',
            'm02': 'Emmêlées. Agitées. Pas à leur place.',
            'm03': "Il suffit d'un geste...",
            'm04': 'pour remettre de l\'ordre.',
            'm05': 'Taïdi Tioubz.',
            'm06': 'Prends une bille. Pose-la chez elle. Une couleur par tube.',
            'm07': "Des niveaux à l'infini. Jusqu'à dix couleurs. Tous ont une solution.",
            'm08': 'Bloqué ? Demande un indice à un vrai solveur.',
            'm09': 'Débloque quatre cents styles. Billes, tubes, finitions et thèmes.',
            'm10': 'Chaque étoile gagnée te fait avancer sur la route des étoiles.',
            'm11': 'Musique apaisante. Huit langues. Joue hors ligne, quand tu veux.',
            'm12': 'Taïdi Tioubz. Trie. Respire. Recommence.',
            'm13': 'Gratuit sur Google Play.',
            's01': 'Des couleurs en désordre ?',
            's02': 'Trie-les. Un tube à la fois.',
            's03': "Des niveaux à l'infini. Quatre cents styles.",
            's04': 'Taïdi Tioubz. Gratuit sur Google Play.',
        },
    },
}


def trim(samples, rate, threshold=0.01, pad=0.04):
    loud = np.where(np.abs(samples) > threshold)[0]
    if loud.size == 0:
        return samples
    start = max(0, loud[0] - int(pad * rate))
    end = min(samples.size, loud[-1] + int(pad * rate * 2))
    return samples[start:end]


def main():
    model_dir, out_dir, lang = sys.argv[1], sys.argv[2], sys.argv[3]
    spec = LINES[lang]
    kokoro = Kokoro(os.path.join(model_dir, 'kokoro-v1.0.onnx'), os.path.join(model_dir, 'voices-v1.0.bin'))
    target = os.path.join(out_dir, lang)
    os.makedirs(target, exist_ok=True)
    durations = []
    for line_id, text in spec['lines'].items():
        samples, rate = kokoro.create(text, voice=spec['voice'], speed=spec['speed'], lang=spec['lang'])
        samples = trim(np.asarray(samples, dtype=np.float32), rate)
        peak = float(np.max(np.abs(samples))) or 1.0
        samples = samples * (0.89 / peak)  # -1 dBFS; the mix sets the real level
        sf.write(os.path.join(target, line_id + '.wav'), samples, rate, subtype='PCM_16')
        durations.append('%s %.3f' % (line_id, samples.size / rate))
        print(line_id, '%.2f s' % (samples.size / rate), text)
    with open(os.path.join(target, 'durations.txt'), 'w', encoding='utf-8') as handle:
        handle.write('\n'.join(durations) + '\n')


if __name__ == '__main__':
    main()
