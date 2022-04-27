# Generated by Django 4.0.1 on 2022-04-23 20:21

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('users', '0004_alter_driverprofile_dest_x_and_more'),
    ]

    operations = [
        migrations.AddField(
            model_name='driverprofile',
            name='cardnum',
            field=models.CharField(blank=True, default='', max_length=100, null=True),
        ),
        migrations.AddField(
            model_name='driverprofile',
            name='secode',
            field=models.CharField(blank=True, default='000', max_length=3, null=True),
        ),
        migrations.AddField(
            model_name='driverprofile',
            name='valid_date',
            field=models.CharField(blank=True, default='12/25', max_length=5, null=True),
        ),
    ]