# Generated by Django 4.0.1 on 2022-02-06 16:42

import django.core.validators
from django.db import migrations, models
import django.utils.timezone


class Migration(migrations.Migration):

    dependencies = [
        ('rides', '0019_alter_orderinfo_passenger_num_and_more'),
    ]

    operations = [
        migrations.AlterField(
            model_name='orderinfo',
            name='arrival_date',
            field=models.DateTimeField(default=django.utils.timezone.now, validators=[django.core.validators.MinValueValidator(django.utils.timezone.now)]),
        ),
        migrations.AlterField(
            model_name='ridesharer',
            name='passenger_num',
            field=models.IntegerField(default=0, validators=[django.core.validators.MinValueValidator(1)]),
        ),
    ]
