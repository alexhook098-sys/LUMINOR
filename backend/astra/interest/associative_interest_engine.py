from dataclasses import dataclass
from typing import List
import random

@dataclass
class AssociativeInterest:
    topic: str
    source_items: List[str]
    reason: str
    score: float

class AssociativeInterestEngine:
    """ASTRA Associative Interest Engine v0.2."""
    def __init__(self, seed=None): self.random=random.Random(seed)
    def _build_question(self, first, second):
        return self.random.choice([
            f"Почему «{first}» и «{second}» могут быть связаны в воспоминании?",
            f"Почему сочетание «{first}» и «{second}» вызывает определённое ощущение?",
            f"Как «{first}» может быть связано с «{second}»?",
            f"Почему «{first}» и «{second}» могут вместе создавать образ прошлого?",
        ])
    def generate(self, memories: List[str]):
        memories=[item for item in memories if item and str(item).strip()]
        if len(memories)<2:return []
        out=[]
        for i in range(len(memories)):
            for j in range(i+1,len(memories)):
                a,b=memories[i],memories[j]
                out.append(AssociativeInterest(self._build_question(a,b),[a,b],"новая связь между элементами памяти",round(0.6+self.random.random()*0.3,3)))
        return sorted(out,key=lambda x:x.score,reverse=True)
    def select(self, memories):
        c=self.generate(memories); return c[0] if c else None
    def explain(self, interest):
        if interest is None:return "Ассоциативный интерес не сформирован."
        return f"Тема: {interest.topic}\nИсточники: {', '.join(interest.source_items)}\nПричина: {interest.reason}\nИнтерес: {interest.score:.3f}"
