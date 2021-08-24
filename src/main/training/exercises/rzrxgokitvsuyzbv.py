from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class rzrxgokitvsuyzbv(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(1805.97,4143.9097,154.48999),
				velocity=Vector3(-497.261,-648.381,1.631),
				angular_velocity=Vector3(4.0514097,-3.12041,-3.13811),
				rotation=Rotator(-0.3992185,2.6759336,-1.2773266))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(3410.3198,4374.77,27.72),
						velocity=Vector3(338.351,-413.871,-5.3009996),
						angular_velocity=Vector3(0.20010999,0.28811002,-2.19901),
						rotation=Rotator(-0.07305583,-1.0578715,-0.24419057)),
					jumped=False,
					double_jumped=False,
					boost_amount=97.0),
				1: CarState(
					physics=Physics(
						location=Vector3(1954.2,3962.3298,102.479996),
						velocity=Vector3(-707.59094,-1282.5609,154.58101),
						angular_velocity=Vector3(4.7958097,-2.65681,-0.43681002),
						rotation=Rotator(-0.8656445,0.72394305,-2.948311)),
					jumped=True,
					double_jumped=True,
					boost_amount=72.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
